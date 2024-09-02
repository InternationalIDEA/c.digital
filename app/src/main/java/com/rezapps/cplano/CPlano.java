package com.rezapps.cplano;


import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;

public class CPlano {

    private static final String TAG = "CPlano" ;

    public ElectionType electionType;
    public CPlanoPage[] pages;
    public String digitalCopyFile = null;

    public CPlano(ElectionType eleType, int pageCount) {
        this.electionType = eleType;
        this.pages = new CPlanoPage[pageCount];
    }

    public boolean isCompleted() {
        for (CPlanoPage page : pages) {
            if (page == null) {
                return false;
            }
        }
        return true;
    }

    public boolean hasDigitalCopy() {
        return digitalCopyFile != null;
    }

    public void store(SharedPreferences pref) {
        SharedPreferences.Editor editor = pref.edit();

        Gson gson = new Gson();
        String json = gson.toJson(this);
        editor.putString(electionType.name(), json);
        editor.commit();

    }

    public static CPlano fromPreference(SharedPreferences pref, ElectionType type) {
        Gson gson = new Gson();
        String json = pref.getString(type.name(), null);
        Log.d(TAG, json);

        CPlano cp = gson.fromJson(json, CPlano.class);
        return cp;
    }

    public int getTotalCandidateVotes() {
        int votes = 0;
        for (CPlanoPage page : pages) {
            if (page != null)
                for (int v : page.verifiedVotes) {
                    votes += v;
                }
        }
        return votes;
    }
}
