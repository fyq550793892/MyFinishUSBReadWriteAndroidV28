package com.example.usbreadwritedemo;

import android.content.Context;
import android.util.Log;


import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        String a = "/recoder";
        int b = a.indexOf("/");
        System.out.println(b);
    }
}