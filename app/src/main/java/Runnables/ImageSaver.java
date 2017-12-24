package Runnables;

import android.media.Image;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Ofek on 24-Dec-17.
 */


public class ImageSaver implements Runnable {
    private Image image;
    OnImageSaved listener;
    File imageFile;


    public ImageSaver(Image image, OnImageSaved listener, File imageFile) {
        this.image = image;
        this.listener = listener;
        this.imageFile = imageFile;
    }

    @Override
    public void run() {
        Log.e("Runnable:  ","started decode");
        ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(imageFile);
            outputStream.write(bytes);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                image.close();
                if (outputStream != null)
                    outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        listener.onImageSaved();
    }

    public interface OnImageSaved {
        void onImageSaved();
    }
}

