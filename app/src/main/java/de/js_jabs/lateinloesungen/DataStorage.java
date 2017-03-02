package de.js_jabs.lateinloesungen;

import java.util.ArrayList;


public class DataStorage {
    private static DataStorage ourInstance = new DataStorage();
    public final String LOG_TAG = "de.js_labs.log";
    public boolean firstStart;
    public boolean userIsEligible;
    public boolean userCanPass;

    public ArrayList<Vokablel> testVocBuffer;
    public Lektion[] lektions = new Lektion[50];

    public boolean removeAds;
    public boolean devMode;
    public boolean surveyMode;
    public boolean firstStartSurveys;
    public boolean extraForms;
    public boolean proveInput;
    public boolean ignoreCase;
    public Long dataTimeStamp;
    public int currentLektion;

    public boolean dataIsLeast = true;

    public static DataStorage getInstance() {
        return ourInstance;
    }

    private DataStorage() {
        firstStart = true;
        userIsEligible = true;
        userCanPass = false;
    }
}
