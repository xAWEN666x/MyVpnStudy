package com.xawen666x.myvpnstudy;

import android.net.Uri;

public class Utils {

    /**
     * Конвертация в string
     *
     * @param resourceId
     * @return путь
     */
    public static String getImgURL(int resourceId) {

        // Use BuildConfig.APPLICATION_ID instead of R.class.getPackage().getName() if both are not same
        return Uri.parse("android.resource://" + R.class.getPackage().getName() + "/" + resourceId).toString();
    }
}
