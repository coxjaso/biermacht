package com.biermacht.brews.recipe;

import android.util.Log;

import com.biermacht.brews.utils.Constants;
import com.biermacht.brews.utils.Units;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.biermacht.brews.ingredient.Fermentable;
import com.biermacht.brews.ingredient.Hop;
import com.biermacht.brews.ingredient.Ingredient;
import com.biermacht.brews.ingredient.Misc;
import com.biermacht.brews.ingredient.Water;
import com.biermacht.brews.ingredient.Yeast;
import com.biermacht.brews.utils.BrewCalculator;
import com.biermacht.brews.utils.InstructionGenerator;
import com.biermacht.brews.utils.Utils;
import com.biermacht.brews.utils.comparators.IngredientComparator;

public class Recipe {
	
	// Beer XML 1.0 Required Fields ===================================
	// ================================================================
	private String name;		     // Recipe name
	private int version;			 // XML Version -- 1
	private String type;             // Extract, Grain, Mash
	private BeerStyle style;         // Stout, Pilsner, etc.
	private String brewer;		     // Brewer's name
	private double batchSize;         // Target size (L)
	private double boilSize;		     // Pre-boil vol (L)
	private int boilTime;		     // In Minutes
	private double efficiency;	     // 100 for extract
	private ArrayList<Hop> hops;     // Hops used
	private ArrayList<Fermentable> fermentables;  // Fermentables used
	private ArrayList<Yeast> yeasts; // Yeasts used
	private ArrayList<Misc> miscs;   // Misc ingredients used
	private ArrayList<Water> waters; // Waters used
    private MashProfile mashProfile; // Mash profile for non-extracts	
	
	// Beer XML 1.0 Optional Fields ===================================
	// ================================================================
	private double OG;			      // Original Gravity
	private double FG;			      // Final Gravity
	private int fermentationStages;   // # of Fermentation stages
	private int primaryAge;			  // Time in primary in days
	private double primaryTemp;		  // Temp in primary in C
	private int secondaryAge;		  // Time in Secondary in days
	private double secondaryTemp;	  // Temp in secondary in C
	private int tertiaryAge;		  // Time in tertiary in days
	private double tertiaryTemp;	  // Temp in tertiary in C
    private String tasteNotes;        // Taste notes
    private int tasteRating;          // Taste score out of 50
    private int bottleAge;            // Bottle age in days
    private double bottleTemp;        // Bottle temp in C
    private boolean isForceCarbonated;// True if force carb is used
    private double carbonation;       // Volumes of carbonation
    private String brewDate;          // Date brewed
    private String primingSugarName;  // Name of sugar for priming
    private double primingSugarEquiv; // Equivalent amount of priming sugar to be used
    private double kegPrimingFactor;  // factor - use less sugar when kegging vs bottles
    private double carbonationTemp;   // Carbonation temperature in C
    private int calories;             // Calories (KiloCals)
	
	// Custom Fields ==================================================
	// ================================================================
	private long id;                  // id for use in database
	private String description;       // User input description
	private int batchTime;            // Total length in weeks
	private double ABV;                // Alcohol by volume
	private double bitterness;         // Bitterness in IBU
	private double color;              // Color - SRM
	private InstructionGenerator instructionGenerator; // Generates instructions
	private double measuredOG;         // Brew day stat: measured OG
	private double measuredFG;         // Brew stat: measured FG
	
	// Static values =================================================
	// ===============================================================
	public static final String EXTRACT = "Extract";
	public static final String ALL_GRAIN = "All Grain";
	public static final String PARTIAL_MASH = "Partial Mash";
	
	public static final int STAGE_PRIMARY = 1;
	public static final int STAGE_SECONDARY = 2;
	public static final int STAGE_TERTIARY = 3;
	
	// Public constructors
	public Recipe(String s)
	{
		// Beer XML 1.0 Required Fields ===================================
		// ================================================================
		this.name = s;	     
		this.setVersion(1);			
		this.setType(EXTRACT);            
		this.style = Constants.BEERSTYLE_OTHER;
		this.setBrewer("Unknown Brewer");		     
		this.setDisplayBatchSize(5);
		this.setDisplayBoilSize(2.5);
        this.setBoilTime(60);
		this.setEfficiency(100);
		this.hops = new ArrayList<Hop>();   
		this.fermentables = new ArrayList<Fermentable>();  
		this.yeasts = new ArrayList<Yeast>(); 
		this.miscs = new ArrayList<Misc>();
		this.waters = new ArrayList<Water>(); 
		this.mashProfile = new MashProfile();
		
		// Beer XML 1.0 Optional Fields ===================================
		// ================================================================
		this.OG = 1;
		this.setFG(1);
		this.setFermentationStages(1);
		this.primaryAge = 14;
		this.secondaryAge = 0;
		this.tertiaryAge = 0;
		this.primaryTemp = 21;
		this.secondaryTemp = 0;
		this.tertiaryTemp = 0;
		
		// Custom Fields ==================================================
		// ================================================================
		this.id = -1;
		this.description = "No description provided";
		this.batchTime = 60;
		this.ABV = 0;
		this.bitterness = 0;
		this.color = 0; 
		this.instructionGenerator = new InstructionGenerator(this);
		this.measuredOG = 0;
		this.measuredFG = 0;
	}

    // Constructor with no arguments!
    public Recipe()
    {
        this("New Recipe");
    }
	
	// Public methods
	public void update()
	{
		setColor(BrewCalculator.calculateColorFromRecipe(this));
		setOG(BrewCalculator.calculateOriginalGravityFromRecipe(this));
		setBitterness(BrewCalculator.calculateIbuFromRecipe(this));
		setFG(BrewCalculator.estimateFinalGravityFromRecipe(this));
		setABV(BrewCalculator.calculateAbvFromRecipe(this));
		this.instructionGenerator.generate();
	}
	
	public void setRecipeName(String name)
	{
		this.name = name;
	}
	
	public String getRecipeName()
	{
		return this.name;
	}
	
	public void addIngredient(Ingredient i)
	{
		if (i.getType().equals(Ingredient.HOP))
			addHop(i);
		else if (i.getType().equals(Ingredient.FERMENTABLE))
			addFermentable(i);
		else if (i.getType().equals(Ingredient.MISC))
			addMisc(i);
		else if (i.getType().equals(Ingredient.YEAST))
			addYeast(i);
		else if (i.getType().equals(Ingredient.WATER))
			addWater(i);
		
		update();
	}
	
	private void removeIngredient(Ingredient i)
	{
		if (i.getType().equals(Ingredient.HOP))
			hops.remove((Hop) i);
		else if (i.getType().equals(Ingredient.FERMENTABLE))
			fermentables.remove((Fermentable) i);
		else if (i.getType().equals(Ingredient.MISC))
			miscs.remove((Misc) i);
		else if (i.getType().equals(Ingredient.YEAST))
			yeasts.remove((Yeast) i);
		else if (i.getType().equals(Ingredient.WATER))
			waters.remove((Water) i);
		
		update();
	}
	
	public MashProfile getMashProfile()
	{
		return this.mashProfile;
	}
	
	public void setMashProfile(MashProfile profile)
	{
		this.mashProfile = profile;
		update();
	}
	
	public String getDescription() 
	{
		return description;
	}

	public void setDescription(String description) 
	{
		if (description.isEmpty())
			this.description = "No description provided.";
		else
			this.description = description;
	}

	public BeerStyle getStyle() 
	{
		return style;
	}

	public void setStyle(BeerStyle beerStyle) 
	{
		this.style = beerStyle;
	}
	
	public ArrayList<Ingredient> getIngredientList()
	{
		ArrayList<Ingredient> list = new ArrayList<Ingredient>();
		list.addAll(hops);
		list.addAll(fermentables);
		list.addAll(yeasts);
		list.addAll(miscs);
		list.addAll(waters);
		
		Collections.sort(list, new IngredientComparator());
		return list;
	}
	
	public void setIngredientsList(ArrayList<Ingredient> ingredientsList) 
	{
		
		for (Ingredient i : ingredientsList)
		{
			if (i.getType().equals(Ingredient.HOP))
				addHop(i);
			else if (i.getType().equals(Ingredient.FERMENTABLE))
				addFermentable(i);
			else if (i.getType().equals(Ingredient.MISC))
				addMisc(i);
			else if (i.getType().equals(Ingredient.YEAST))
				addYeast(i);
			else if (i.getType().equals(Ingredient.WATER))
				addWater(i);
		}
		
		update();
	}
	
	private void addWater(Ingredient i) 
	{
		Water w = (Water) i;
		waters.add(w);
	}

	private void addYeast(Ingredient i) 
	{
		Yeast y = (Yeast) i;
		yeasts.add(y);
	}

	private void addMisc(Ingredient i) 
	{
		Misc m = (Misc) i;
		miscs.add(m);
	}

	private void addFermentable(Ingredient i) 
	{
		Fermentable f = (Fermentable) i;
		fermentables.add(f);
	}

	private void addHop(Ingredient i) 
	{
		Hop h = (Hop) i;
		hops.add(h);
	}

	public ArrayList<Instruction> getInstructionList()
	{
		return this.instructionGenerator.getInstructions();
	}

	public double getOG() 
	{
		return OG;
	}

	public void setOG(double gravity) 
	{
		gravity = (double) Math.round(gravity * 1000) / 1000;
		this.OG = gravity;
	}

	public double getBitterness() 
	{
		bitterness = (double) Math.round(bitterness * 10) / 10;
		return bitterness;
	}

	public void setBitterness(double bitterness) 
	{
		bitterness = (double) Math.round(bitterness * 10) / 10;
		this.bitterness = bitterness;
	}

	public double getColor() 
	{
		color = (double) Math.round(color * 10) / 10;
		return color;
	}

	public void setColor(double color) 
	{
		color = (double) Math.round(color * 10) / 10;
		this.color = color;
	}

	public double getABV() 
	{
		ABV = (double) Math.round(ABV * 10) / 10;
		return ABV;
	}

	public void setABV(double aBV) 
	{
		ABV = (double) Math.round(ABV * 10) / 10;
		ABV = aBV;
	}

	public int getBatchTime() 
	{
		return batchTime;
	}

	public void setBatchTime(int batchTime) 
	{
		this.batchTime = batchTime;
	}

	public long getId() 
	{
		return id;
	}

	public void setId(long id) 
	{
		this.id = id;
	}
	
	public double getDisplayBatchSize() 
	{
		return Units.litersToGallons(this.batchSize);
	}
	
	public void setDisplayBatchSize(double size) 
	{
		this.batchSize = Units.gallonsToLiters(size);
	}

	public double getBeerXmlStandardBatchSize() 
	{
		return this.batchSize;
	}
	
	public void setBeerXmlStandardBatchSize(double v)
	{
		this.batchSize = v;
	}

	public int getBoilTime() 
	{
		return boilTime;
	}

	public void setBoilTime(int boilTime) 
	{
		this.boilTime = boilTime;
	}
	
	public double getEfficiency() 
	{
		return efficiency;
	}

	public void setEfficiency(double efficiency) 
	{
		this.efficiency = efficiency;
	}
	
	@Override
	public String toString()
	{
		return this.getRecipeName();
	}
	
	/**
	 * @return the brewer
	 */
	public String getBrewer() 
	{
		return brewer;
	}

	/**
	 * @param brewer the brewer to set
	 */
	public void setBrewer(String brewer) 
	{
		this.brewer = brewer;
	}
	
	public double getDisplayBoilSize()
	{
		return Units.litersToGallons(this.boilSize);
	}
	
	public void setDisplayBoilSize(double size) 
	{
		this.boilSize = Units.gallonsToLiters(size);
	}

	/**
	 * @return the boilSize
	 */
	public double getBeerXmlStandardBoilSize() 
	{
		return boilSize;
	}

	/**
	 * @param boilSize the boilSize to set
	 */
	public void setBeerXmlStandardBoilSize(double boilSize) 
	{
		this.boilSize = boilSize;
	}

	/**
	 * @return the type
	 */
	public String getType() 
	{
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) 
	{
		this.type = type;
	}

	/**
	 * @return the fG
	 */
	public double getFG() {
		this.FG = (double) Math.round(FG * 1000) / 1000;
		return this.FG;
	}

	/**
	 * @param fG the fG to set
	 */
	public void setFG(double fG) {
		fG = (double) Math.round(fG * 1000) / 1000;
		this.FG = fG;
	}

	/**
	 * @return the fermentationStages
	 */
	public int getFermentationStages() 
	{
		return fermentationStages;
	}

	/**
	 * @param fermentationStages the fermentationStages to set
	 */
	public void setFermentationStages(int fermentationStages) 
	{
		this.fermentationStages = fermentationStages;
	}

	/**
	 * @return the version
	 */
	public int getVersion() 
	{
		return version;
	}

	/**
	 * @param version the version to set
	 */
	public void setVersion(int version) 
	{
		this.version = version;
	}
	
	public ArrayList<Misc> getMiscList()
	{
		return miscs;
	}
	
	public ArrayList<Fermentable> getFermentablesList()
	{
		return fermentables;
	}
	
	public ArrayList<Hop> getHopsList()
	{
		return hops;
	}
	
	public ArrayList<Yeast> getYeastsList()
	{
		return yeasts;
	}
	
	public double getMeasuredOG()
	{
		return this.measuredOG;
	}
	
	public double getMeasuredFG()
	{
		return this.measuredFG;
	}
	
	public void setMeasuredOG(double d)
	{
		this.measuredOG = d;
	}
	
	public void setMeasuredFG(double d)
	{
		this.measuredFG = d;
	}
	
	public int getDisplayCoolToFermentationTemp()
	{
		for (Yeast y : this.getYeastsList())
		{
			return y.getDisplayFermentationTemp();
		}
		return 70;
	}
	
	public void setNumberFermentationStages(int stages)
	{
		this.fermentationStages = stages;
	}
	
	public void setFermentationAge(int stage, int age)
	{
		switch (stage)
		{
			case STAGE_PRIMARY:
				this.primaryAge = age;
			case STAGE_SECONDARY:
				this.secondaryAge = age;
			case STAGE_TERTIARY:
				this.tertiaryAge = age;
		}
	}
	
	public int getFermentationAge(int stage)
	{
		switch (stage)
		{
			case STAGE_PRIMARY:
				return this.primaryAge;
			case STAGE_SECONDARY:
				return this.secondaryAge;
			case STAGE_TERTIARY:
				return this.tertiaryAge;
			default:
				return 7;
		}
	}
	
	public void setBeerXmlStandardFermentationTemp(int stage, double temp)
	{
		switch (stage)
		{
			case STAGE_PRIMARY:
				this.primaryTemp = temp;
			case STAGE_SECONDARY:
				this.secondaryTemp = temp;
			case STAGE_TERTIARY:
				this.tertiaryTemp = temp;
		}
	}
	
	public double getBeerXmlStandardFermentationTemp(int stage)
	{
		switch (stage)
		{
			case STAGE_PRIMARY:
				return this.primaryTemp;
			case STAGE_SECONDARY:
				return this.secondaryTemp;
			case STAGE_TERTIARY:
				return this.tertiaryTemp;
			default:
				return 21;
		}
	}
	
	public void setDisplayFermentationTemp(int stage, double temp)
	{
		switch (stage)
		{
			case STAGE_PRIMARY:
				this.primaryTemp = Units.farenheitToCelsius(temp);
			case STAGE_SECONDARY:
				this.secondaryTemp = Units.farenheitToCelsius(temp);
			case STAGE_TERTIARY:
				this.tertiaryTemp = Units.farenheitToCelsius(temp);
		}
	}
	
	public double getDisplayFermentationTemp(int stage)
	{
		switch (stage)
		{
			case STAGE_PRIMARY:
				return Units.celsiusToFarenheit(this.primaryTemp);
			case STAGE_SECONDARY:
				return Units.celsiusToFarenheit(this.secondaryTemp);
			case STAGE_TERTIARY:
				return Units.celsiusToFarenheit(this.tertiaryTemp);
			default:
				return Units.celsiusToFarenheit(21);
		}
	}

    public String getTasteNotes()
    {
        return this.tasteNotes;
    }

    public void setTasteNotes(String s)
    {
        this.tasteNotes = s;
    }

    public int getTasteRating()
    {
        return this.tasteRating;
    }

    public void setTasteRating(int i)
    {
        this.tasteRating = i;
    }

    public int getBottleAge()
    {
        return this.bottleAge;
    }

    public void setBottleAge(int i)
    {
        this.bottleAge = i;
    }

    public double getBeerXmlStandardBottleTemp()
    {
        return this.bottleTemp;
    }

    public void setBeerXmlStandardBottleTemp(double d)
    {
        this.bottleTemp = d;
    }

    public void setDisplayBottleTemp(double d)
    {
        this.bottleTemp = Units.farenheitToCelsius(d);
    }

    public double getDisplayBottleTemp()
    {
        return Units.celsiusToFarenheit(this.bottleTemp);
    }

    public boolean isForceCarbonated()
    {
        return this.isForceCarbonated;
    }

    public void setIsForceCarbonated(boolean b)
    {
        this.isForceCarbonated = b;
    }

    public double getCarbonation()
    {
        return this.carbonation;
    }

    public void setCarbonation (double d)
    {
        this.carbonation = d;
    }

    public String getBrewDate()
    {
        return this.brewDate;
    }

    public void setBrewDate(String s)
    {
        this.brewDate = s;
    }

    public String getPrimingSugarName()
    {
        return this.primingSugarName;
    }

    public void setPrimingSugarName(String s)
    {
        this.primingSugarName = s;
    }

    public double getPrimingSugarEquiv()
    {
        // TODO
        return 0.0;
    }

    public void setPrimingSugarEquiv(double d)
    {
        this.primingSugarEquiv = d;
    }

    public double getBeerXmlStandardCarbonationTemp()
    {
        return this.carbonationTemp;
    }

    public void setBeerXmlStandardCarbonationTemp(double d)
    {
        this.carbonationTemp = d;
    }

    public void setDisplayCarbonationTemp(double d)
    {
        this.carbonationTemp = Units.farenheitToCelsius(d);
    }

    public double getDisplayCarbonationTemp()
    {
        return Units.celsiusToFarenheit(this.carbonationTemp);
    }

    public double getKegPrimingFactor()
    {
        return this.kegPrimingFactor;
    }

    public void setKegPrimingFactor(double d)
    {
        this.kegPrimingFactor = d;
    }

    public int getCalories()
    {
        return this.calories;
    }

    public void setCalories(int i)
    {
        this.calories = i;
    }

    public double getMeasuredABV()
    {
        if (this.getMeasuredFG() > 0 && this.getMeasuredOG() > this.getMeasuredFG())
            return (this.getMeasuredOG() - this.getMeasuredFG()) * 131;
        else
            return 0;
    }

    public double getMeasuredEfficiency()
    {
        double gravP, measGravP;
        double eff = 100;

        if (!this.getType().equals(Recipe.EXTRACT))
            eff = getEfficiency();

        if (this.getMeasuredFG() > 0 && this.getMeasuredOG() > this.getMeasuredFG())
        {
            gravP = (BrewCalculator.calculateOriginalGravityFromRecipe(this)-1)/(eff/100);
            measGravP = this.getMeasuredOG() - 1;
            Log.d("Recipe", " calcd: " + gravP + " meas: " + measGravP + " eff: " + eff);
            return 100 * measGravP / gravP;
        }
        else
            return 0;
    }
}
