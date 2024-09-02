package com.rezapps.cdigital;

import android.graphics.Bitmap;

import com.rezapps.cplano.CPlanoPage;
import com.rezapps.cplano.ElectionType;

import java.io.IOException;

public interface CPlanoReaderOld {

    public CPlanoPage read(Bitmap photo, String fileName, ElectionType electType, int dapil, int pageNum)
            throws IOException;
}
