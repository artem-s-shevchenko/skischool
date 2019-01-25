package ch.epfl.esl.sportstracker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataMap;
import com.google.firebase.auth.FirebaseAuth;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

class Profile implements Serializable {

    String email;
    String password;
    String phoneNumber;
    String username;
    String photoPath;
    PersonType person;
    String userID = "";
    String myTeacher;
    String userLevel;

    Profile(String username, String password, PersonType personType) {
        // When you create a new Profile, it's good to build it based on email and password
        this.email = username;
        this.password = password;
        this.person   = personType;
    }

    Profile(String username, PersonType personType) {
        // When you create a new Profile, it's good to build it based on email and password
        this.email = username;
        this.person   = personType;
    }

    DataMap toDataMap() {

        DataMap dataMap = new DataMap();
        dataMap.putString("email", email);
        dataMap.putString("password", password);
        dataMap.putString("phoneNumber", phoneNumber);
        dataMap.putString("username", username);
        dataMap.putString("userID",userID);
        final InputStream imageStream;
        try {
            imageStream = new FileInputStream(photoPath);
            final Bitmap userImage = BitmapFactory.decodeStream(imageStream);
            Asset asset = WearService.createAssetFromBitmap(userImage);
            dataMap.putAsset("photo", asset);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return dataMap;
    }
}
