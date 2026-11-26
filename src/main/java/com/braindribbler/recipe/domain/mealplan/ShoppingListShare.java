package com.braindribbler.recipe.domain.mealplan;

import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.domain.recipe.AccessLevel;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "shopping_list_shares")
public class ShoppingListShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shopping_list_share_id")
    private Integer shoppingListShareId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shopping_list_id", nullable = false)
    private ShoppingList shoppingList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_user_id", nullable = false)
    private User sharedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "access_level_id", nullable = false)
    private AccessLevel accessLevel;

    @Column(name = "shared_at", nullable = false, updatable = false)
    private OffsetDateTime sharedAt = OffsetDateTime.now();

    public ShoppingListShare() {}

    public Integer getShoppingListShareId() { return shoppingListShareId; }
    public void setShoppingListShareId(Integer shoppingListShareId) { this.shoppingListShareId = shoppingListShareId; }

    public ShoppingList getShoppingList() { return shoppingList; }
    public void setShoppingList(ShoppingList shoppingList) { this.shoppingList = shoppingList; }

    public User getSharedUser() { return sharedUser; }
    public void setSharedUser(User sharedUser) { this.sharedUser = sharedUser; }

    public AccessLevel getAccessLevel() { return accessLevel; }
    public void setAccessLevel(AccessLevel accessLevel) { this.accessLevel = accessLevel; }

    public OffsetDateTime getSharedAt() { return sharedAt; }
    public void setSharedAt(OffsetDateTime sharedAt) { this.sharedAt = sharedAt; }
}
