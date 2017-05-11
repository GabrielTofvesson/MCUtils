package org.teamavion.util.support;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

@SuppressWarnings({"unused", "unchecked"})
public final class Log {

    private static Logger logger;
    private static OutputStream out = new FileOutputStream(FileDescriptor.out);

    public static void setLogger(Logger log) { logger = log; }
    public static void log(Level level, Object message) { logger.log(level, ">>> " + message.toString()); }
    public static void info(Object message) { log(Level.INFO, message); }
    public static void warning(Object message) { log(Level.WARN, message); }
    public static void error(Object message) { log(Level.ERROR, message); }
    public static <T> void array(Level level, T... array) { log(level, Arrays.toString(array)); }

    public static void write(int i){
        try {
            out.write(i);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Direct console access methods (bypasses any bootstrapped printstreams or the like)
    public static void write(byte[] b){ for(byte b1 : b) write(b1); }
    public static void print(String s){ write(s.getBytes()); }
    public static void print(boolean b){ print(String.valueOf(b)); }
    public static void print(byte b){ print(String.valueOf(b)); }
    public static void print(char c){ print(String.valueOf(c)); }
    public static void print(short s){ print(String.valueOf(s)); }
    public static void print(int i){ print(String.valueOf(i)); }
    public static void print(long l){ print(String.valueOf(l)); }
    public static void print(float f){ print(String.valueOf(f)); }
    public static void print(double d){ print(String.valueOf(d)); }
    public static void println(String s){ write((s + '\r' + '\n').getBytes()); }
    public static void println(boolean b){ println(String.valueOf(b)); }
    public static void println(byte b){ println(String.valueOf(b)); }
    public static void println(char c){ println(String.valueOf(c)); }
    public static void println(short s){ println(String.valueOf(s)); }
    public static void println(int i){ println(String.valueOf(i)); }
    public static void println(long l){ println(String.valueOf(l)); }
    public static void println(float f){ println(String.valueOf(f)); }
    public static void println(double d){ println(String.valueOf(d)); }
    public static void println(){ write("\r\n".getBytes()); }
}
