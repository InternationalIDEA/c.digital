package com.rezapps.cdigital;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.rezapps.cplano.CPlano;
import com.rezapps.cplano.ElectionType;
import com.rezapps.cplano.Templates;
import com.rezapps.ocrform.FormTemplate;

import java.util.HashMap;
import java.util.Map;

public class CDigitalApplication extends Application {

    public static String TAG = "CDigital";

    public static int DPD_CANDIDATES_PER_PAGE = 6;

    // public Map<ElectionType, String[]> paslons;
    // private Map<ElectionType, String[]> partais;
    //private Map<ElectionType, String[][]> calonpartais;
    // private Map<ElectionType, Integer> dapils;
    public Templates templates;

    Gson gson = new Gson();

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences partiesPref = this.getSharedPreferences(Param.PARTIES, MODE_PRIVATE);
        SharedPreferences.Editor partiesEditor = partiesPref.edit();
        partiesEditor.putString(ElectionType.DPR_RI.name(),
                "['Partai Alpha', 'Partai Beta', 'Partai Gamma']");

        partiesEditor.putString(ElectionType.DPRD_PROV.name(),
                "['Partai Alpha', 'Partai Beta', 'Partai Gamma', 'Partai Delta', 'Partai Epsilon']");

        partiesEditor.putString(ElectionType.DPRD_KAB.name(),
                "['Partai Alpha', 'Partai Beta', 'Partai Gamma']");

        partiesEditor.commit();




        new Thread() {
            public void run() {
                backgroundInitialization();
            }
        }.start();

    }

    private void backgroundInitialization() {


        SharedPreferences candidatesPref = this.getSharedPreferences(Param.CANDIDATES, MODE_PRIVATE);
        SharedPreferences.Editor candidatesEditor = candidatesPref.edit();
        candidatesEditor.putString(ElectionType.PRESIDEN.name(),
                "['Andy dan Bambang', 'Cynthia dan Dony', 'Edi dan Fajri']");

        candidatesEditor.putString(ElectionType.DPR_RI.name(), gson.toJson(new String[][]{
                {"Alpha A", "Alpha B", "Alpha C", "Alpha D", "Alpha E", "Alpha F", "Alpha G", "Alpha H"},
                {"Beta A", "Beta B", "Beta C", "Beta D", "Beta E", "Beta F", "Beta G", "Beta H"},
                {"Gamma A", "Gamma B", "Gamma C", "Gamma D", "Gamma E", "Gamma F", "Gamma G", "Gamma H"}
        }));

        candidatesEditor.putString(ElectionType.DPRD_PROV.name(), gson.toJson(new String[][]{
                {"Alpha A", "Alpha B", "Alpha C", "Alpha D", "Alpha E", "Alpha F", "Alpha G", "Alpha H", "Alpha I", "Alpha J"},
                {"Beta A", "Beta B", "Beta C", "Beta D", "Beta E", "Beta F", "Beta G", "Beta H", "Beta I", "Beta J"},
                {"Gamma A", "Gamma B", "Gamma C", "Gamma D", "Gamma E", "Gamma F", "Gamma G", "Gamma H", "Gamma I"},
                {"Delta A", "Delta B", "Delta C", "Delta D", "Delta E", "Delta F", "Delta G", "Delta H"},
                {"Epsilon A", "Epsilon B", "Epsilon C", "Epsilon D", "Epsilon E", "Epsilon F", "Epsilon G"}
        }));

        candidatesEditor.putString(ElectionType.DPRD_KAB.name(), gson.toJson(new String[][]{
                {"Alpha A", "Alpha B", "Alpha C", "Alpha D", "Alpha E", "Alpha F", "Alpha G", "Alpha H", "Alpha I", "Alpha J",
                        "Alpha K", "Alpha L", "Alpha M", "Alpha N"},
                {"Beta A", "Beta B", "Beta C", "Beta D", "Beta E", "Beta F", "Beta G", "Beta H", "Beta I", "Beta J" ,
                        "Beta K", "Beta L", "Beta M", "Beta N"},
                {"Gamma A", "Gamma B", "Gamma C", "Gamma D", "Gamma E", "Gamma F", "Gamma G", "Gamma H", "Gamma I", "Gamma J" ,
                        "Gamma K", "Gamma L", "Gamma M", "Gamma N"}
        }));


        candidatesEditor.putString(ElectionType.DPD_RI.name(), gson.toJson(new String[]{
                "Calon DPD Satu",
                "Calon DPD Dua",
                "Calon DPD Tiga",
                "Calon DPD Empat",
                "Calon DPD Lima",
                "Calon DPD Enam",
                "Calon DPD Tujuh",
                "Calon DPD Delapan",
                "Calon DPD Sembilan",
                "Calon DPD Sepuluh"
        }));

        candidatesEditor.commit();

        templates = new Templates(this);


        SharedPreferences cPlanoPref = this.getSharedPreferences(Param.C_PLANO, MODE_PRIVATE);

        // Initiate CPlano data
        for (ElectionType electionType : ElectionType.values()) {
            if (!cPlanoPref.contains(electionType.name())) {
                int pageCount;
                if (electionType == ElectionType.PRESIDEN)
                    pageCount = 2;
                else if (electionType == ElectionType.DPD_RI) {
                    int candidatesCount = getCandidates(electionType).length;
                    pageCount = (int)Math.ceil((float)candidatesCount / DPD_CANDIDATES_PER_PAGE) + 2;
                } else
                    pageCount = getParties(electionType).length + 2;

                Log.i(TAG, "Create dummy cPlano for " + electionType + ", pages=" + pageCount);

                CPlano cPlano = new CPlano(electionType, pageCount);
                cPlano.store(cPlanoPref);
            }
        }

    }

    public String[] getParties(ElectionType type) {
        SharedPreferences pref = this.getSharedPreferences(Param.PARTIES, MODE_PRIVATE);
        String json = pref.getString(type.name(), "[]");
        return gson.fromJson(json, String[].class );
    }

    public String[] getCandidates(ElectionType type) {
        SharedPreferences pref = this.getSharedPreferences(Param.CANDIDATES, MODE_PRIVATE);
        String json = pref.getString(type.name(), "[]");
        return gson.fromJson(json, String[].class );
    }

    public String[] getPartyCandidates(ElectionType type, int partyIdx) {
        SharedPreferences pref = this.getSharedPreferences(Param.CANDIDATES, MODE_PRIVATE);
        String json = pref.getString(type.name(), "[]");
        String[][] partyCandidates = gson.fromJson(json, String[][].class );
        if (partyCandidates.length > partyIdx) {
            return partyCandidates[partyIdx];
        } else {
            return new String[0];
        }
    }

    public FormTemplate determineTemplate(ElectionType electionType, int pageNum) {
        if (pageNum == 1) {
            return templates.get(Templates.DPPHP);

        } else if (electionType == ElectionType.PRESIDEN) {
            return templates.get(Templates.PWP3);

        } else if (electionType == ElectionType.DPD_RI) {
            int candidatesCount = getCandidates(electionType).length;
            int votePageCount = (int)Math.ceil((float)candidatesCount / DPD_CANDIDATES_PER_PAGE);
            if (pageNum < votePageCount + 2)
                return templates.get(Templates.DPD);
            else
                return templates.get(Templates.DPX_J);

        } else if (electionType.dpr) {
            int partyCount = getParties(electionType).length;

            if (pageNum >= partyCount + 2)
                return templates.get(Templates.DPX_J);

            int maxCandidateCount = 0;
            for (int i=0; i < partyCount; i++) {
                int candidateCount = getPartyCandidates(electionType, i).length;
                if (candidateCount > maxCandidateCount)
                    maxCandidateCount = candidateCount;
            }

            if (maxCandidateCount <= 8)
                return templates.get(Templates.DPRX8);
            else if (maxCandidateCount <= 10)
                return templates.get(Templates.DPRX10);
            else
                return templates.get(Templates.DPRX14);

        } else {
            return null;
        }
    }
}
