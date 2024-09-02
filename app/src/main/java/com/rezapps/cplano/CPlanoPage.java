package com.rezapps.cplano;

import com.rezapps.ocrform.FieldEntry;
import com.rezapps.ocrform.Form;

import java.util.HashMap;
import java.util.Map;

public class CPlanoPage {

    public enum PageType {
        DATA_PPHP,
        SUARA_PWP,
        SUARA_DPRX,
        SUARA_DPD,

        SUARA_TOTAL
    }

    public Form form;
    int pageNum;
    PageType type;

    public String alignedPhotoFile;

    public Map<String,Integer> data = new HashMap<>();
    public Map<String,Integer> verifiedData = new HashMap<>();
    public int[] votes = new int[0];
    public int[] verifiedVotes;


    public CPlanoPage(ElectionType electType, int pageNum) {
        this.pageNum = pageNum;

        // determine pageType
        if (pageNum == 1)
            this.type = PageType.DATA_PPHP;
        else if (pageNum == 18)
            this.type = PageType.SUARA_TOTAL;
        else {
            if (electType == ElectionType.PRESIDEN)
                this.type = PageType.SUARA_PWP;
            else
                this.type = PageType.SUARA_DPRX;
        }
    }

    public CPlanoPage(Form form, int pageNum, String alignedPhotoFile) {
        this.pageNum = pageNum;
        this.alignedPhotoFile = alignedPhotoFile;
        this.form = form;

        if (form.templateName.equals(Templates.DPPHP)) type = PageType.DATA_PPHP;
        else if (form.templateName.equals(Templates.PWP3)) type = PageType.SUARA_PWP;
        else if (form.templateName.equals(Templates.DPRX8)) type = PageType.SUARA_DPRX;
        else if (form.templateName.equals(Templates.DPRX10)) type = PageType.SUARA_DPRX;
        else if (form.templateName.equals(Templates.DPRX12)) type = PageType.SUARA_DPRX;
        else if (form.templateName.equals(Templates.DPRX14)) type = PageType.SUARA_DPRX;
        else if (form.templateName.equals(Templates.DPD)) type = PageType.SUARA_DPD;
        else if (form.templateName.equals(Templates.DPX_J)) type = PageType.SUARA_TOTAL;

        for (String fieldName : form.getFieldNames()) {
            FieldEntry[] entries = form.getFieldEntries(fieldName);
            if (fieldName.equals(FieldName.CALON) || fieldName.equals(FieldName.PASLON)) {
                votes = new int[entries.length];
                for (int i=0; i < entries.length; i++) {
                    votes[i] = entries[i].getCombinedValue();
                }

            } else {
                data.put(fieldName, entries[0].getCombinedValue());
            }

            verifiedVotes = votes.clone();
            verifiedData.putAll(data);
        }

    }

    public int getPageNum() {
        return pageNum;
    }

    public PageType getType() {
        return type;
    }

    public int getData(String fieldName) {
        return data.get(fieldName);
    }

    public int getVerifiedData(String fieldName) {
        return verifiedData.get(fieldName);
    }

    public void putVerifiedData(String fieldName, int value) {
        verifiedData.put(fieldName, value);
    }

    public int getVote(int idx) {
        return votes[idx];
    }

    public int getVerifiedVote(int idx) {
        return verifiedVotes[idx];
    }

    public void setVerifiedVote(int idx, int value) {
        verifiedVotes[idx] = value;
    }

}
