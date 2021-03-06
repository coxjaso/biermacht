package com.biermacht.brews.xml;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.biermacht.brews.ingredient.Fermentable;
import com.biermacht.brews.ingredient.Hop;
import com.biermacht.brews.ingredient.Misc;
import com.biermacht.brews.ingredient.Water;
import com.biermacht.brews.ingredient.Yeast;
import com.biermacht.brews.recipe.BeerStyle;
import com.biermacht.brews.recipe.MashProfile;
import com.biermacht.brews.recipe.MashStep;
import com.biermacht.brews.recipe.Recipe;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class RecipeXmlWriter {
  private Context c;
  private String lastFileLocation;

  public RecipeXmlWriter(Context c) {
    this.c = c;
    this.lastFileLocation = "";
  }

  public void writeRecipe(Recipe r, String filePrefix) {
    ArrayList<Recipe> list = new ArrayList<Recipe>();
    list.add(r);
    this.writeRecipes(list, filePrefix);
  }

  public Document generateDocument(List<Recipe> list) throws ParserConfigurationException {
    // Open the document.
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder;

    // This throws the exceptions!
    docBuilder = docFactory.newDocumentBuilder();

    // Create root element.
    Document doc = docBuilder.newDocument();
    Element rootElement = doc.createElement("RECIPES");
    doc.appendChild(rootElement);

    for (Recipe r : list) {
      // Element for this recipe.
      Element recipeElement = doc.createElement("RECIPE");

      // Create a mapping of name -> value
      Map<String, String> map = new HashMap<String, String>();
      map.put("NAME", r.getRecipeName());
      map.put("VERSION", r.getVersion() + "");
      map.put("TYPE", r.getType());
      map.put("EQUIPMENT", "");
      map.put("BREWER", "");
      map.put("BATCH_SIZE", r.getBeerXmlStandardBatchSize() + "");
      map.put("BOIL_SIZE", r.getBeerXmlStandardBoilSize() + "");
      map.put("BOIL_TIME", r.getBoilTime() + "");
      map.put("EFFICIENCY", r.getEfficiency() + "");
      map.put("NOTES", r.getNotes());
      map.put("OG", r.getOG() + "");
      map.put("FG", r.getFG() + "");
      map.put("DISPLAY_OG", r.getMeasuredOG() + "");
      map.put("DISPLAY_FG", r.getMeasuredFG() + "");
      map.put("FERMENTATION_STAGES", r.getFermentationStages() + "");
      map.put("PRIMARY_AGE", r.getFermentationAge(Recipe.STAGE_PRIMARY) + "");
      map.put("SECONDARY_AGE", r.getFermentationAge(Recipe.STAGE_SECONDARY) + "");
      map.put("TERTIARY_AGE", r.getFermentationAge(Recipe.STAGE_TERTIARY) + "");
      map.put("PRIMARY_TEMP", r.getBeerXmlStandardFermentationTemp(Recipe.STAGE_PRIMARY) + "");
      map.put("SECONDARY_TEMP", r.getBeerXmlStandardFermentationTemp(Recipe.STAGE_SECONDARY) + "");
      map.put("TERTIARY_TEMP", r.getBeerXmlStandardFermentationTemp(Recipe.STAGE_TERTIARY) + "");
      map.put("AGE", r.getBottleAge() + "");

      for (Map.Entry<String, String> e : map.entrySet()) {
        String fieldName = e.getKey();
        String fieldValue = e.getValue();
        Element element = doc.createElement(fieldName);
        element.setTextContent(fieldValue);
        recipeElement.appendChild(element);
      }

      // Add elements to recipe.
      recipeElement.appendChild(this.getHopsChild(doc, r.getHopsList()));
      recipeElement.appendChild(this.getFermentablesChild(doc, r.getFermentablesList()));
      recipeElement.appendChild(this.getMiscsChild(doc, r.getMiscList()));
      recipeElement.appendChild(this.getYeastsChild(doc, r.getYeastsList()));
      recipeElement.appendChild(this.getWatersChild(doc, r.getWatersList()));
      recipeElement.appendChild(this.getMashChild(doc, r.getMashProfile()));
      recipeElement.appendChild(this.getStyleChild(doc, r.getStyle()));

      // Add recipe to root <RECIPES> element.
      rootElement.appendChild(recipeElement);
    }
    return doc;
  }

  public String getXmlText(List<Recipe> list) {
    // Get the generated XML doc.
    Document doc;
    try {
      doc = generateDocument(list);
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
      return null;
    }

    DOMSource domSource = new DOMSource(doc);
    StringWriter writer = new StringWriter();
    StreamResult result = new StreamResult(writer);
    TransformerFactory tf = TransformerFactory.newInstance();
    Transformer transformer = null;

    try {
      transformer = tf.newTransformer();
    } catch (TransformerConfigurationException e) {
      e.printStackTrace();
    }

    try {
      transformer.transform(domSource, result);
    } catch (TransformerException e) {
      e.printStackTrace();
    }

    writer.flush();
    return writer.toString();
  }

  public static String generateFileName(String prefix) {
    // Generate date string
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    String dateString = dateFormat.format(new Date());

    return prefix + dateString + ".xml";
  }

  public void writeRecipes(List<Recipe> list, String filePrefix) {
    // Get the generated XML doc.
    Document doc;
    try {
      doc = generateDocument(list);
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
      return;
    }

    // Write to XML file.
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = null;
    try {
      transformer = transformerFactory.newTransformer();
    } catch (TransformerConfigurationException e) {
      e.printStackTrace();
      return;
    }
    DOMSource source = new DOMSource(doc);

    // Create file object
    File file = null;
    try {
      file = getStoragePath(generateFileName(filePrefix));
    } catch (IOException e) {
      e.printStackTrace();
    }
    StreamResult result = new StreamResult(file);
    Log.d("WriteXmlFile", "Writing XML to:" + file);

    try {
      transformer.transform(source, result);
    } catch (TransformerException e) {
      e.printStackTrace();
    }
    this.lastFileLocation = file.getAbsolutePath();
  }

  /**
   * Creates and returns a style document for the given BeerStyle object.
   */
  public Element getStyleChild(Document d, BeerStyle s) {
    // Create the element.
    Element rootElement = d.createElement("STYLE");

    // Create a mapping of name -> value
    Map<String, String> map = new HashMap<String, String>();
    map.put("NAME", s.getName());
    map.put("CATEGORY", s.getCategory());
    map.put("VERSION", "1"); // FIXME: Actually set version.
    map.put("CATEGORY_NUMBER", s.getCatNum() + "");
    map.put("STYLE_LETTER", s.getStyleLetter());
    map.put("STYLE_GUIDE", s.getStyleGuide());
    map.put("TYPE", s.getType());
    map.put("OG_MIN", String.format("%2.8f", s.getMinOg()));
    map.put("OG_MAX", String.format("%2.8f", s.getMaxOg()));
    map.put("FG_MIN", String.format("%2.8f", s.getMinFg()));
    map.put("FG_MAX", String.format("%2.8f", s.getMaxFg()));
    map.put("IBU_MIN", String.format("%2.8f", s.getMinIbu()));
    map.put("IBU_MAX", String.format("%2.8f", s.getMaxIbu()));
    map.put("COLOR_MIN", String.format("%2.8f", s.getMinColor()));
    map.put("COLOR_MAX", String.format("%2.8f", s.getMaxColor()));
    map.put("CARB_MIN", String.format("%2.8f", s.getMinCarb()));
    map.put("CARB_MAX", String.format("%2.8f", s.getMaxCarb()));
    map.put("ABV_MIN", String.format("%2.8f", s.getMinAbv()));
    map.put("ABV_MAX", String.format("%2.8f", s.getMaxAbv()));
    map.put("NOTES", s.getNotes());
    map.put("PROFILE", s.getProfile());
    map.put("INGREDIENTS", s.getIngredients());
    map.put("EXAMPLES", s.getExamples());

    for (Map.Entry<String, String> e : map.entrySet()) {
      String fieldName = e.getKey();
      String fieldValue = e.getValue();
      Element element = d.createElement(fieldName);
      element.setTextContent(fieldValue);
      rootElement.appendChild(element);
    }

    return rootElement;
  }

  public Element getHopsChild(Document d, ArrayList<Hop> l) {
    // Create the element.
    Element hopsElement = d.createElement("HOPS");

    for (Hop h : l) {
      Element hopElement = d.createElement("HOP");

      // Create fields of element
      Element nameElement = d.createElement("NAME");
      Element versionElement = d.createElement("VERSION");
      Element alphaElement = d.createElement("ALPHA");
      Element amountElement = d.createElement("AMOUNT");
      Element useElement = d.createElement("USE");
      Element timeElement = d.createElement("TIME");
      Element notesElement = d.createElement("NOTES");
      Element typeElement = d.createElement("TYPE");
      Element formElement = d.createElement("FORM");

      // Assign values
      nameElement.setTextContent(h.getName());
      versionElement.setTextContent(h.getVersion() + "");
      alphaElement.setTextContent(h.getAlphaAcidContent() + "");
      amountElement.setTextContent(h.getBeerXmlStandardAmount() + "");
      timeElement.setTextContent(h.getTime() + "");
      notesElement.setTextContent(h.getDescription());
      typeElement.setTextContent(h.getType());
      formElement.setTextContent(h.getForm());
      useElement.setTextContent(h.getUse());

      // Attach to element.
      hopElement.appendChild(nameElement);
      hopElement.appendChild(versionElement);
      hopElement.appendChild(alphaElement);
      hopElement.appendChild(amountElement);
      hopElement.appendChild(useElement);
      hopElement.appendChild(timeElement);
      hopElement.appendChild(notesElement);
      hopElement.appendChild(typeElement);
      hopElement.appendChild(formElement);

      // Attach to list of elements.
      hopsElement.appendChild(hopElement);
    }

    return hopsElement;
  }

  public Element getFermentablesChild(Document d, ArrayList<Fermentable> l) {
    // Create the element.
    Element fermentablesElement = d.createElement("FERMENTABLES");

    for (Fermentable f : l) {
      Element fermentableElement = d.createElement("FERMENTABLE");

      // Create fields of element
      Element nameElement = d.createElement("NAME");
      Element versionElement = d.createElement("VERSION");
      Element typeElement = d.createElement("TYPE");
      Element amountElement = d.createElement("AMOUNT");
      Element yieldElement = d.createElement("YIELD");
      Element colorElement = d.createElement("COLOR");
      Element addAfterBoilElement = d.createElement("ADD_AFTER_BOIL");

      // Assign values
      nameElement.setTextContent(f.getName());
      versionElement.setTextContent(f.getVersion() + "");
      typeElement.setTextContent(f.getFermentableType());
      amountElement.setTextContent(f.getBeerXmlStandardAmount() + "");
      yieldElement.setTextContent(f.getYield() + "");
      colorElement.setTextContent(f.getLovibondColor() + "");
      addAfterBoilElement.setTextContent(f.isAddAfterBoil() + "");

      // Attach to element.
      fermentableElement.appendChild(nameElement);
      fermentableElement.appendChild(versionElement);
      fermentableElement.appendChild(typeElement);
      fermentableElement.appendChild(amountElement);
      fermentableElement.appendChild(yieldElement);
      fermentableElement.appendChild(colorElement);
      fermentableElement.appendChild(addAfterBoilElement);

      // Attach to list of elements.
      fermentablesElement.appendChild(fermentableElement);
    }

    return fermentablesElement;
  }

  public Element getMiscsChild(Document d, ArrayList<Misc> l) {
    // Create the element.
    Element miscsElement = d.createElement("MISCS");

    for (Misc m : l) {
      miscsElement.appendChild(this.getMiscChild(d, m));
    }

    return miscsElement;
  }

  public Element getMiscChild(Document d, Misc m) {
    // Create the element.
    Element rootElement = d.createElement("MISC");

    // Create a mapping of name -> value
    Map<String, String> map = new HashMap<String, String>();
    map.put("NAME", m.getName());
    map.put("VERSION", m.getVersion() + "");
    map.put("TYPE", m.getType());
    map.put("USE", m.getUse());
    map.put("AMOUNT", String.format("%2.8f", m.getBeerXmlStandardAmount()));
    map.put("DISPLAY_AMOUNT", m.getDisplayAmount() + " " + m.getDisplayUnits());
    map.put("DISPLAY_TIME", m.getTime() + " " + m.getTimeUnits());
    map.put("AMOUNT_IS_WEIGHT", m.amountIsWeight() ? "true" : "false");
    map.put("NOTES", m.getShortDescription());
    map.put("USE_FOR", m.getUseFor());

    for (Map.Entry<String, String> e : map.entrySet()) {
      String fieldName = e.getKey();
      String fieldValue = e.getValue();
      Element element = d.createElement(fieldName);
      element.setTextContent(fieldValue);
      rootElement.appendChild(element);
    }

    return rootElement;
  }

  public Element getYeastsChild(Document d, ArrayList<Yeast> l) {
    // Create the element.
    Element yeastsElement = d.createElement("YEASTS");

    for (Yeast y : l) {
      Element yeastElement = d.createElement("YEAST");

      // Create fields of element
      Element nameElement = d.createElement("NAME");
      Element versionElement = d.createElement("VERSION");
      Element typeElement = d.createElement("TYPE");
      Element formElement = d.createElement("FORM");
      Element amountElement = d.createElement("AMOUNT");
      Element laboratoryElement = d.createElement("LABORATORY");
      Element productIdElement = d.createElement("PRODUCT_ID");
      Element minTempElement = d.createElement("MIN_TEMPERATURE");
      Element maxTempElement = d.createElement("MAX_TEMPERATURE");
      Element attenuationElement = d.createElement("ATTENUATION");

      // Assign values
      nameElement.setTextContent(y.getName());
      versionElement.setTextContent(y.getVersion() + "");
      typeElement.setTextContent(y.getType());
      formElement.setTextContent(y.getForm());
      amountElement.setTextContent(y.getBeerXmlStandardAmount() + "");
      laboratoryElement.setTextContent(y.getLaboratory());
      productIdElement.setTextContent(y.getProductId());
      minTempElement.setTextContent(y.getBeerXmlStandardFermentationTemp() + "");
      maxTempElement.setTextContent(y.getBeerXmlStandardFermentationTemp() + "");
      attenuationElement.setTextContent(y.getAttenuation() + "");

      // Attach to element.
      yeastElement.appendChild(nameElement);
      yeastElement.appendChild(versionElement);
      yeastElement.appendChild(typeElement);
      yeastElement.appendChild(amountElement);
      yeastElement.appendChild(laboratoryElement);
      yeastElement.appendChild(productIdElement);
      yeastElement.appendChild(minTempElement);
      yeastElement.appendChild(maxTempElement);
      yeastElement.appendChild(attenuationElement);

      // Attach to list of elements.
      yeastsElement.appendChild(yeastElement);
    }

    return yeastsElement;
  }

  public Element getWatersChild(Document d, ArrayList<Water> l) {
    return d.createElement("WATERS");
  }

  public Element getMashChild(Document d, MashProfile m) {
    // Create the element.
    Element rootElement = d.createElement("MASH");

    // Create a mapping of name -> value
    Map<String, String> map = new HashMap<String, String>();
    map.put("NAME", m.getName());
    map.put("VERSION", m.getVersion() + "");
    map.put("GRAIN_TEMP", String.format("%2.8f", m.getBeerXmlStandardGrainTemp()));
    map.put("TUN_TEMP", String.format("%2.8f", m.getBeerXmlStandardTunTemp()));
    map.put("SPARGE_TEMP", String.format("%2.8f", m.getBeerXmlStandardSpargeTemp()));
    map.put("NOTES", m.getNotes());
    map.put("PH", String.format("%2.8f", m.getpH()));
    map.put("TUN_WEIGHT", String.format("%2.8f", m.getBeerXmlStandardTunWeight()));
    map.put("TUN_SPECIFIC_HEAT", String.format("%2.8f", m.getBeerXmlStandardTunSpecHeat()));
    map.put("EQUIP_ADJUST", "FALSE"); // FIXME: Actually set this.

    for (Map.Entry<String, String> e : map.entrySet()) {
      String fieldName = e.getKey();
      String fieldValue = e.getValue();
      Element element = d.createElement(fieldName);
      element.setTextContent(fieldValue);
      rootElement.appendChild(element);
    }

    // Add mash steps
    rootElement.appendChild(getMashStepsChild(d, m));

    return rootElement;
  }

  public Element getMashStepsChild(Document d, MashProfile m) {
    // Create the element.
    Element rootElement = d.createElement("MASH_STEPS");

    for (MashStep s : m.getMashStepList()) {
      rootElement.appendChild(getMashStepChild(d, s));
    }

    return rootElement;
  }

  public Element getMashStepChild(Document d, MashStep s) {
    // Create the element.
    Element rootElement = d.createElement("MASH_STEP");

    // Create a mapping of name -> value
    Map<String, String> map = new HashMap<String, String>();
    map.put("NAME", s.getName());
    map.put("VERSION", s.getVersion() + "");
    map.put("TYPE", s.getType());
    map.put("INFUSE_AMOUNT", String.format("%2.8f", s.getBeerXmlStandardInfuseAmount()));
    map.put("STEP_TEMP", String.format("%2.8f", s.getBeerXmlStandardStepTemp()));
    map.put("STEP_TIME", String.format("%2.8f", s.getStepTime()));
    map.put("RAMP_TIME", String.format("%2.8f", s.getRampTime()));
    map.put("END_TIME", String.format("%2.8f", s.getBeerXmlStandardEndTemp()));

    for (Map.Entry<String, String> e : map.entrySet()) {
      String fieldName = e.getKey();
      String fieldValue = e.getValue();
      Element element = d.createElement(fieldName);
      element.setTextContent(fieldValue);
      rootElement.appendChild(element);
    }

    return rootElement;
  }

  public File getStoragePath(String fileName) throws IOException {
    // Get the directory for the app's private directory on the SD card.  If there is none, try
    // the internal storage directory.
    File sdCard = Environment.getExternalStorageDirectory();
    File filePath = new File(sdCard.getAbsolutePath() + "/BiermachtBrews");
    File file = new File(filePath.getAbsolutePath() + "/", fileName);
    Log.d("XmlWriter", "Attempting to create file: " + file.getAbsolutePath());
    if (! filePath.mkdirs()) {
      Log.e("XmlWriter", "Cannot create directory.");
    }
    if (! file.exists()) {
      if (! file.createNewFile()) {
        Log.e("XmlWriter", "File not created.");
      }
    }
    else {
      Log.d("XmlWriter", "File already exists.");
    }
    return file;
  }

  public String getSavedFileLocation() {
    return this.lastFileLocation;
  }
}
