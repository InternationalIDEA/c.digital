package com.rezapps.cplano;

import com.rezapps.ocrform.Form;
import com.rezapps.ocrform.FormTemplate;

public class CPlanoInfo {

    String qrCode;
    ElectionType electionType;
    int dapil;
    int pageNum;

    char templateCode;

    public CPlanoInfo(String qrCode) {
        this.qrCode = qrCode;
        int electionCode = Integer.parseInt(qrCode.substring(0,1));
        electionType = ElectionType.fromCode(electionCode);

        dapil = Integer.parseInt(qrCode.substring(1,6));
        pageNum = Integer.parseInt(qrCode.substring(7,9));

        templateCode = qrCode.charAt(6);
    }

    public String getQrCode() {
        return qrCode;
    }

    public int getDapil() {
        return dapil;
    }

    public int getPageNum() {
        return pageNum;
    }

    public ElectionType getElectionType() {
        return electionType;
    }

    public char getTemplateCode() {
        return templateCode;
    }


}
