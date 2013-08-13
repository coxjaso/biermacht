package com.biermacht.brews.frontend.IngredientActivities;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;

import com.biermacht.brews.R;
import com.biermacht.brews.exceptions.RecipeNotFoundException;
import com.biermacht.brews.ingredient.Fermentable;
import com.biermacht.brews.utils.Constants;
import com.biermacht.brews.utils.Database;

public class EditCustomFermentableActivity extends AddCustomFermentableActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable delete button for this view
        findViewById(R.id.delete_button).setVisibility(View.VISIBLE);
    }

    @Override
    public void getValuesFromIntent()
    {
        // Get the recipe from calling activity
        super.getValuesFromIntent();

        // Get the ingredient as well
        long grainId = getIntent().getLongExtra(Constants.INTENT_INGREDIENT_ID, Constants.INVALID_ID);
        fermentable = (Fermentable) Database.getIngredientWithId(grainId);
    }

    @Override
    public void getIngredientList()
    {
        // Get ingredient list
        super.getIngredientList();
    }

    @Override
    public void setValues()
    {
        nameViewText.setText(fermentable.getName());
        colorViewText.setText(String.format("%2.2f", fermentable.getLovibondColor()));
        gravityViewText.setText(String.format("%2.3f", fermentable.getGravity()));
        descriptionViewText.setText(fermentable.getShortDescription());
        timeViewText.setText("60");
        amountViewText.setText("1");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_add_ingredient, menu);
        return true;
    }

    @Override
    public void onDeletePressed()
    {
        Database.deleteIngredientWithId(ingredientId, Constants.INGREDIENT_DB_CUSTOM);
        finish();
    }

    @Override
    public void onFinished()
    {
        Database.deleteIngredientWithId(ingredientId, Constants.MASTER_RECIPE_ID);
        Database.addIngredientToVirtualDatabase(Constants.INGREDIENT_DB_CUSTOM, fermentable, Constants.MASTER_RECIPE_ID);
        finish();
    }
}
