package com.rezapps.cplano;

public enum ElectionType {
    PRESIDEN    ("PWP","Pemilihan Presiden dan Wakil Presiden",
                "Pemilihan Presiden dan Wakil Presiden Republik Indonesia", false, 9),
    DPR_RI      ("DPR", "Pemilihan Anggota DPR RI",
                "Pemilihan Anggota Dewan Perwakilan Rakyat Republik Indonesia", true, 1),
    DPRD_PROV   ("DPRP","Pemilihan Anggota DPRD Provinsi",
                "Pemilihan Anggota Dewan Perwakilan Rakyat Daerah Provinsi", true, 2),
    DPRD_KAB    ("DPRK", "Pemilihan Anggota DPRD Kab/Kota",
                "Pemilihan Anggota Dewan Perwakilan Rakyat Daerah Kabupaten", true, 3),
    DPD_RI       ("DPD", "Pemilihan Anggota DPD RI",
                 "Pemilihan Anggota Dewan Perwakilan Daerah Republik Indonesia", false, 4);

    public final String abbrev;
    public final String desc;
    public final String fullDesc;
    public final boolean dpr;
    public final int code;

    ElectionType(String abbrev, String desc, String fullDesc, boolean dpr, int code) {

        this.abbrev = abbrev;
        this.desc = desc;
        this.fullDesc = fullDesc;
        this.dpr = dpr;
        this.code = code;
    }

    public static ElectionType fromCode(int code) {
        switch (code) {
            case 1:
                return DPR_RI;
            case 2:
                return DPRD_PROV;
            case 3:
                return DPRD_KAB;
            case 4:
                return DPD_RI;
            case 9:
            case 0:
                return PRESIDEN;
        }
        return null;
    }

}
