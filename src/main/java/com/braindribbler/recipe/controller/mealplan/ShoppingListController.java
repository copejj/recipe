package com.braindribbler.recipe.controller.mealplan;

import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.domain.mealplan.ShoppingList;
import com.braindribbler.recipe.domain.mealplan.ShoppingListItem;
import com.braindribbler.recipe.domain.recipe.GroceryCategory;
import com.braindribbler.recipe.domain.recipe.Measurement;
import com.braindribbler.recipe.repository.recipe.GroceryCategoryRepository;
import com.braindribbler.recipe.repository.recipe.MeasurementRepository;
import com.braindribbler.recipe.security.RecipeUserDetails;
import com.braindribbler.recipe.service.mealplan.MealPlanService;
import com.braindribbler.recipe.service.mealplan.ShoppingListService;
import com.braindribbler.recipe.service.mealplan.ShoppingListSseService;
import com.braindribbler.recipe.service.recipe.IngredientService;
import com.braindribbler.recipe.service.recipe.MetadataLookupService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/shopping-lists")
public class ShoppingListController {

    private final ShoppingListService shoppingListService;
    private final ShoppingListSseService shoppingListSseService;
    private final MealPlanService mealPlanService;
    private final IngredientService ingredientService;
    private final MetadataLookupService metadataLookupService;
    private final MeasurementRepository measurementRepository;
    private final GroceryCategoryRepository groceryCategoryRepository;
    private final TransactionTemplate transactionTemplate;

    public ShoppingListController(ShoppingListService shoppingListService,
                                  ShoppingListSseService shoppingListSseService,
                                  MealPlanService mealPlanService,
                                  IngredientService ingredientService,
                                  MetadataLookupService metadataLookupService,
                                  MeasurementRepository measurementRepository,
                                  GroceryCategoryRepository groceryCategoryRepository,
                                  TransactionTemplate transactionTemplate) {
        this.shoppingListService = shoppingListService;
        this.shoppingListSseService = shoppingListSseService;
        this.mealPlanService = mealPlanService;
        this.ingredientService = ingredientService;
        this.metadataLookupService = metadataLookupService;
        this.measurementRepository = measurementRepository;
        this.groceryCategoryRepository = groceryCategoryRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @GetMapping
    public String listShoppingLists(Model model, @AuthenticationPrincipal RecipeUserDetails currentUser) {
        if (currentUser == null) return "redirect:/login";
        User user = currentUser.getUserProfile();
        model.addAttribute("shoppingLists", shoppingListService.getAccessibleShoppingLists(user.getUserId()));
        model.addAttribute("currentUser", user);
        return "shopping-lists/index";
    }

    @PostMapping("/create")
    public String createShoppingList(@RequestParam String listName,
                                     @AuthenticationPrincipal RecipeUserDetails currentUser,
                                     RedirectAttributes redirectAttributes) {
        if (currentUser == null) return "redirect:/login";
        if (listName == null || listName.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please enter a name for your shopping list.");
            return "redirect:/shopping-lists";
        }
        ShoppingList created = shoppingListService.createStandaloneList(currentUser.getUserProfile(), listName);
        return "redirect:/shopping-lists/" + created.getPublicId();
    }

    @GetMapping("/{publicId}")
    public String viewShoppingList(@PathVariable UUID publicId,
                                   Model model,
                                   @AuthenticationPrincipal RecipeUserDetails currentUser) {
        if (currentUser == null) return "redirect:/login";
        User user = currentUser.getUserProfile();
        ShoppingList shoppingList = shoppingListService.getByPublicId(publicId);

        boolean canEdit = canUserEditList(shoppingList, user);
        boolean isOwner = shoppingList.getOwner().getUserId().equals(user.getUserId());
        boolean isShared = shoppingList.getMealPlan() != null
                && shoppingList.getMealPlan().getShares() != null
                && !shoppingList.getMealPlan().getShares().isEmpty();

        boolean canAccess = canEdit || (shoppingList.getMealPlan() != null
                && mealPlanService.canUserView(shoppingList.getMealPlan(), user));

        if (!canAccess) {
            return "redirect:/meal-plans?unauthorized";
        }

        model.addAttribute("shoppingList", shoppingList);
        model.addAttribute("canEdit", canEdit);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("isShared", isShared);
        model.addAttribute("itemsByAisle", shoppingListService.groupItemsByAisle(shoppingList));
        model.addAttribute("groceryCategories", ingredientService.getAllGroceryCategoriesSortedByOrder());
        model.addAttribute("measurements", metadataLookupService.getAllMeasurements());
        model.addAttribute("allIngredients", ingredientService.getAllIngredientsSorted(user));

        return "shopping-lists/view";
    }

    @PostMapping("/{publicId}/delete")
    public String deleteShoppingList(@PathVariable UUID publicId,
                                     @AuthenticationPrincipal RecipeUserDetails currentUser,
                                     RedirectAttributes redirectAttributes) {
        if (currentUser == null) return "redirect:/login";
        User user = currentUser.getUserProfile();
        ShoppingList list = shoppingListService.getByPublicId(publicId);

        if (!list.getOwner().getUserId().equals(user.getUserId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only the owner can delete this shopping list.");
            return "redirect:/shopping-lists/" + publicId;
        }

        String name = list.getListName();
        shoppingListService.deleteShoppingList(list);
        redirectAttributes.addFlashAttribute("successMessage", "Deleted shopping list \"" + name + "\".");
        return "redirect:/";
    }

    @GetMapping(value = "/{publicId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamShoppingList(@PathVariable UUID publicId,
                                                         @AuthenticationPrincipal RecipeUserDetails currentUser) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        User user = currentUser.getUserProfile();

        transactionTemplate.setReadOnly(true);
        Boolean canAccess = transactionTemplate.execute(status -> {
            ShoppingList list = shoppingListService.getByPublicId(publicId);
            return canUserAccessList(list, user);
        });

        if (!Boolean.TRUE.equals(canAccess)) {
            return ResponseEntity.status(403).build();
        }

        SseEmitter emitter = shoppingListSseService.subscribe(publicId);
        return ResponseEntity.ok()
                .header("X-Accel-Buffering", "no")
                .header("Cache-Control", "no-cache, no-transform")
                .body(emitter);
    }
    @PostMapping("/{publicId}/items/add")
    public String addExtraStoreItem(@PathVariable UUID publicId,
                                    @RequestParam(required = false) Integer ingredientId,
                                    @RequestParam(required = false) String customItemName,
                                    @RequestParam(required = false) Integer groceryCategoryId,
                                    @RequestParam(required = false) BigDecimal amount,
                                    @RequestParam(required = false) Integer measurementId,
                                    @RequestParam(required = false) String note,
                                    @AuthenticationPrincipal RecipeUserDetails currentUser,
                                    RedirectAttributes redirectAttributes) {
        if (currentUser == null) return "redirect:/login";
        User user = currentUser.getUserProfile();
        ShoppingList list = shoppingListService.getByPublicId(publicId);

        if (!canUserEditList(list, user)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You have view-only access to this shopping list.");
            return "redirect:/shopping-lists/" + publicId;
        }

        if (ingredientId == null && (customItemName == null || customItemName.isBlank())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please enter an item name.");
            return "redirect:/shopping-lists/" + publicId;
        }

        GroceryCategory category = (groceryCategoryId != null) ? groceryCategoryRepository.findById(groceryCategoryId).orElse(null) : null;
        Measurement unit = (measurementId != null) ? measurementRepository.findById(measurementId).orElse(null) : null;

        shoppingListService.addExtraItem(list, ingredientId, customItemName, category, amount, unit, note, user);
        shoppingListSseService.broadcastStructureChanged(publicId);

        redirectAttributes.addFlashAttribute("successMessage", "Added item to shopping list.");
        return "redirect:/shopping-lists/" + publicId;
    }

    @PostMapping("/{publicId}/items/{itemId}/delete")
    public String deleteItem(@PathVariable UUID publicId,
                             @PathVariable Integer itemId,
                             @AuthenticationPrincipal RecipeUserDetails currentUser,
                             RedirectAttributes redirectAttributes) {
        if (currentUser == null) return "redirect:/login";
        ShoppingList list = shoppingListService.getByPublicId(publicId);

        if (!canUserEditList(list, currentUser.getUserProfile())) {
            redirectAttributes.addFlashAttribute("errorMessage", "You have view-only access to this shopping list.");
            return "redirect:/shopping-lists/" + publicId;
        }

        shoppingListService.deleteItem(itemId);
        shoppingListSseService.broadcastStructureChanged(publicId);

        return "redirect:/shopping-lists/" + publicId;
    }

    @PostMapping("/{publicId}/reset")
    public String resetShoppingList(@PathVariable UUID publicId,
                                    @AuthenticationPrincipal RecipeUserDetails currentUser,
                                    RedirectAttributes redirectAttributes) {
        if (currentUser == null) return "redirect:/login";
        User user = currentUser.getUserProfile();
        ShoppingList list = shoppingListService.getByPublicId(publicId);

        if (!canUserEditList(list, user)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You have view-only access to this shopping list.");
            return "redirect:/shopping-lists/" + publicId;
        }

        shoppingListService.resetFromMealPlan(list, user);
        shoppingListSseService.broadcastStructureChanged(publicId);

        redirectAttributes.addFlashAttribute("successMessage", "Shopping list reset from meal plan!");
        return "redirect:/shopping-lists/" + publicId;
    }

    @GetMapping("/{publicId}/state")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getShoppingListState(@PathVariable UUID publicId,
                                                                    @AuthenticationPrincipal RecipeUserDetails currentUser) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        User user = currentUser.getUserProfile();
        ShoppingList list = shoppingListService.getByPublicId(publicId);

        if (!canUserAccessList(list, user)) {
            return ResponseEntity.status(403).build();
        }

        List<Map<String, Object>> itemsState = list.getItems().stream()
                .map(i -> Map.<String, Object>of(
                        "id", i.getShoppingListItemId(),
                        "checked", Boolean.TRUE.equals(i.getIsChecked()),
                        "questioned", Boolean.TRUE.equals(i.getIsQuestioned()),
                        "hidden", Boolean.TRUE.equals(i.getIsHidden())
                ))
                .toList();

        return ResponseEntity.ok(Map.of(
                "count", itemsState.size(),
                "items", itemsState
        ));
    }

    private boolean canUserAccessList(ShoppingList list, User user) {
        if (user == null || list == null) return false;
        if (canUserEditList(list, user)) return true;
        return list.getMealPlan() != null && mealPlanService.canUserView(list.getMealPlan(), user);
    }

    private boolean canUserEditList(ShoppingList list, User user) {
        if (user == null || list == null) return false;
        if (list.getOwner().getUserId().equals(user.getUserId())) return true;
        return list.getMealPlan() != null && mealPlanService.canUserEdit(list.getMealPlan(), user);
    }

    @PostMapping("/{publicId}/items/{itemId}/toggle")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleItem(@PathVariable UUID publicId,
                                                          @PathVariable Integer itemId,
                                                          @AuthenticationPrincipal RecipeUserDetails currentUser) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        User user = currentUser.getUserProfile();
        ShoppingList list = shoppingListService.getByPublicId(publicId);

        if (!canUserAccessList(list, user)) {
            return ResponseEntity.status(403).build();
        }

        ShoppingListItem updated = shoppingListService.toggleItemChecked(itemId);
        boolean isChecked = Boolean.TRUE.equals(updated.getIsChecked());
        boolean isQuestioned = Boolean.TRUE.equals(updated.getIsQuestioned());
        shoppingListSseService.broadcastItemToggled(publicId, itemId, isChecked, isQuestioned);

        return ResponseEntity.ok(Map.of(
                "itemId", itemId,
                "isChecked", isChecked,
                "isQuestioned", isQuestioned
        ));
    }

    @PostMapping("/{publicId}/items/{itemId}/question")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleItemQuestion(@PathVariable UUID publicId,
                                                                  @PathVariable Integer itemId,
                                                                  @AuthenticationPrincipal RecipeUserDetails currentUser) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        User user = currentUser.getUserProfile();
        ShoppingList list = shoppingListService.getByPublicId(publicId);

        if (!canUserAccessList(list, user)) {
            return ResponseEntity.status(403).build();
        }

        ShoppingListItem item = list.getItems().stream()
                .filter(i -> i.getShoppingListItemId().equals(itemId))
                .findFirst()
                .orElse(null);
        String itemName = (item != null) ? item.getDisplayName() : "Item";

        boolean isQuestioned = shoppingListService.toggleItemQuestioned(itemId);
        shoppingListSseService.broadcastItemQuestioned(publicId, itemId, isQuestioned, itemName);

        return ResponseEntity.ok(Map.of("itemId", itemId, "isQuestioned", isQuestioned));
    }

    @PostMapping("/{publicId}/items/{itemId}/hide")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleItemHidden(@PathVariable UUID publicId,
                                                                @PathVariable Integer itemId,
                                                                @AuthenticationPrincipal RecipeUserDetails currentUser) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        User user = currentUser.getUserProfile();
        ShoppingList list = shoppingListService.getByPublicId(publicId);

        if (!canUserEditList(list, user)) {
            return ResponseEntity.status(403).build();
        }

        boolean isHidden = shoppingListService.toggleItemHidden(itemId);
        shoppingListSseService.broadcastItemHidden(publicId, itemId, isHidden);

        return ResponseEntity.ok(Map.of("itemId", itemId, "isHidden", isHidden));
    }
}
