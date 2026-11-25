package com.braindribbler.recipe.service.mealplan;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ShoppingListSseService {

    private final Map<UUID, List<SseEmitter>> emittersByList = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID publicId) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
        emittersByList.computeIfAbsent(publicId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> removeEmitter(publicId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            cleanup.run();
        }

        return emitter;
    }

    public void broadcastItemToggled(UUID publicId, Integer itemId, boolean isChecked) {
        sendEvent(publicId, "item-toggled", Map.of("id", itemId, "checked", isChecked));
    }

    public void broadcastStructureChanged(UUID publicId) {
        sendEvent(publicId, "structure-changed", Map.of("reload", true));
    }

    private void sendEvent(UUID publicId, String eventName, Object payload) {
        List<SseEmitter> emitters = emittersByList.get(publicId);
        if (emitters == null || emitters.isEmpty()) return;

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(payload));
            } catch (IOException e) {
                removeEmitter(publicId, emitter);
            }
        }
    }

    private void removeEmitter(UUID publicId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByList.get(publicId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                emittersByList.remove(publicId, emitters);
            }
        }
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 25000)
    public void sendKeepAliveHeartbeat() {
        emittersByList.forEach((publicId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("keepalive"));
                } catch (IOException e) {
                    removeEmitter(publicId, emitter);
                }
            }
        });
    }

    public void broadcastItemToggled(UUID publicId, Integer itemId, boolean isChecked, boolean isQuestioned) {
        sendEvent(publicId, "item-toggled", Map.of(
                "id", itemId,
                "checked", isChecked,
                "questioned", isQuestioned
        ));
    }

    public void broadcastItemQuestioned(UUID publicId, Integer itemId, boolean isQuestioned, String itemName) {
        sendEvent(publicId, "item-questioned", Map.of(
                "id", itemId,
                "questioned", isQuestioned,
                "name", itemName != null ? itemName : "Item"
        ));
    }

    public void broadcastItemHidden(UUID publicId, Integer itemId, boolean isHidden) {
        sendEvent(publicId, "item-hidden", Map.of(
                "id", itemId,
                "hidden", isHidden
        ));
    }
}
