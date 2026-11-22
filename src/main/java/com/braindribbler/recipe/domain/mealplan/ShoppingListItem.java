package com.braindribbler.recipe.domain.mealplan;

import com.braindribbler.recipe.domain.auth.User;
import com.braindribbler.recipe.domain.recipe.GroceryCategory;
import com.braindribbler.recipe.domain.recipe.Ingredient;
import com.braindribbler.recipe.domain.recipe.Measurement;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "shopping_list_items")
public class ShoppingListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shopping_list_item_id")
    private Integer shoppingListItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shopping_list_id", nullable = false)
    private ShoppingList shoppingList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;

    @Column(name = "custom_item_name", columnDefinition = "text")
    private String customItemName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grocery_category_id", nullable = false)
    private GroceryCategory groceryCategory;

    @Column(name = "amount", precision = 8, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "measurement_id")
    private Measurement measurement;

    @Column(name = "note", columnDefinition = "text")
    private String note;

    @Column(name = "is_checked", nullable = false)
    private Boolean isChecked = false;

    @Column(name = "is_manual_addition", nullable = false)
    private Boolean isManualAddition = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by_user_id")
    private User addedBy;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 1;

    public ShoppingListItem() {}

    @Transient
    public String getDisplayName() {
        return (ingredient != null) ? ingredient.getIngredientName() : customItemName;
    }

    // Getters and Setters
    public Integer getShoppingListItemId() { return shoppingListItemId; }
    public void setShoppingListItemId(Integer shoppingListItemId) { this.shoppingListItemId = shoppingListItemId; }

    public ShoppingList getShoppingList() { return shoppingList; }
    public void setShoppingList(ShoppingList shoppingList) { this.shoppingList = shoppingList; }

    public Ingredient getIngredient() { return ingredient; }
    public void setIngredient(Ingredient ingredient) { this.ingredient = ingredient; }

    public String getCustomItemName() { return customItemName; }
    public void setCustomItemName(String customItemName) { this.customItemName = customItemName; }

    public GroceryCategory getGroceryCategory() { return groceryCategory; }
    public void setGroceryCategory(GroceryCategory groceryCategory) { this.groceryCategory = groceryCategory; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Measurement getMeasurement() { return measurement; }
    public void setMeasurement(Measurement measurement) { this.measurement = measurement; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Boolean getIsChecked() { return isChecked; }
    public void setIsChecked(Boolean isChecked) { this.isChecked = isChecked; }

    public Boolean getIsManualAddition() { return isManualAddition; }
    public void setIsManualAddition(Boolean isManualAddition) { this.isManualAddition = isManualAddition; }

    public User getAddedBy() { return addedBy; }
    public void setAddedBy(User addedBy) { this.addedBy = addedBy; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
