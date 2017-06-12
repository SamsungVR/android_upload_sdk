/*
 * Copyright (c) 2016 Samsung Electronics America
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.samsung.msca.samsungvr.sampleapp;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

class Util {

    static final boolean DEBUG = BuildConfig.DEBUG;

    static String getLogTag(Object obj) {
        Class<?> cls = null;
        if (obj instanceof Class<?>) {
            cls = (Class<?>)obj;
        } else {
            cls = obj.getClass();
        }
        return "VRSampleApp." + cls.getSimpleName();
    }

    static final String ACTION_SHOW_LOGIN_PAGE = BuildConfig.APPLICATION_ID + ".showLoginPage";
    static final String ACTION_SHOW_LOGGED_IN_PAGE = BuildConfig.APPLICATION_ID + ".loggedIn";
    static final String ACTION_LOGOUT = BuildConfig.APPLICATION_ID + ".logout";

    static final String ACTION_SHOW_CREATE_ACCOUNT_PAGE = BuildConfig.APPLICATION_ID + ".createAccount";
    static final String EXTRA_SHOW_LOGGED_IN_PAGE_ARGS = BuildConfig.APPLICATION_ID + ".loggedIn.args";


    static void sendBroadcast(LocalBroadcastManager mgr, String action, String extra, Bundle extras) {
        Intent intent = new Intent(action);
        if (null != extra && null != extras) {
            intent.putExtra(extra, extras);
        }
        mgr.sendBroadcast(intent);
    }

    static void showLoginPage(LocalBroadcastManager mgr) {
        sendBroadcast(mgr, ACTION_SHOW_LOGIN_PAGE, null, null);
    }

    static void showCreateAccountPage(LocalBroadcastManager mgr) {
        sendBroadcast(mgr, ACTION_SHOW_CREATE_ACCOUNT_PAGE, null, null);
    }

    static void showLoggedInPage(LocalBroadcastManager mgr, Bundle args) {
        sendBroadcast(mgr, ACTION_SHOW_LOGGED_IN_PAGE, EXTRA_SHOW_LOGGED_IN_PAGE_ARGS, args);
    }

    static boolean launchDocPicker(Fragment fragment, int resultCode) {

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("*/*");
        intent.setFlags(intent.getFlags() | Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            fragment.startActivityForResult(intent, resultCode);
            return true;
        } catch (Exception ex) {
            Toast.makeText(fragment.getActivity(), R.string.no_document_picker_activity, Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    public static void setEnabled(List<View> viewGroupStack, View v, boolean enabled) {

        if (null == viewGroupStack) {
            viewGroupStack = new ArrayList<>();
        }
        viewGroupStack.clear();
        viewGroupStack.add(v);

        while (viewGroupStack.size() > 0) {
            View current = viewGroupStack.remove(0);
            current.setEnabled(enabled);
            if (current instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup)current;
                for (int i = vg.getChildCount() - 1; i >= 0; i -= 1) {
                    viewGroupStack.add(vg.getChildAt(i));
                }
            }
        }
        viewGroupStack.clear();
    }


    static void copy(OutputStream output, InputStream input, byte[] buf) throws Exception {
        int len;

        while ((len = input.read(buf)) > 0) {
            output.write(buf, 0, len);
        }

    }

    private static String bytesToStr(byte b[], int offset, int len) {
        if (len <= 2048) {
            return new String(b, offset, Math.min(len, 1024));
        }
        return "too large to show";
    }

    static class DebugOutputStream extends FilterOutputStream {

        private static final String TAG = Util.getLogTag(DebugOutputStream.class);

        private DebugOutputStream(OutputStream base) {
            super(base);
        }

        private OutputStream getBase() {
            return out;
        }


        @Override
        public void write(byte[] buffer, int offset, int length) throws IOException {
            super.write(buffer, offset, length);
            String temp = bytesToStr(buffer, offset, length);
            Log.d(TAG, "Write buffer offset: " + offset +
                    " count: " + length + " val:\n" + temp);
        }

        @Override
        public void write(int oneByte) throws IOException {
            super.write(oneByte);
            //if (DEBUG) {
                //Log.d(TAG, "Write byte: " + oneByte);
            //}
        }

        @Override
        public void write(byte[] buffer) throws IOException {
            super.write(buffer);
            int len = buffer.length;
            String temp = bytesToStr(buffer, 0, len);
            Log.d(TAG, "Write buffer count: " + len + " val:\n" + temp);
        }
    }

    static class DebugInputStream extends FilterInputStream {

        private static final String TAG = Util.getLogTag(DebugInputStream.class);

        private DebugInputStream(InputStream base) {
            super(base);
        }

        private InputStream getBase() {
            return in;
        }

        @Override
        public int read(byte[] buffer) throws IOException {
            int result = super.read(buffer);
            String temp = bytesToStr(buffer, 0, result);
            Log.d(TAG, "Read buffer len: " + result +
                    " val:\n" + temp);
            return result;
        }

        @Override
        public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
            int result = super.read(buffer, byteOffset, byteCount);
            String temp = bytesToStr(buffer, 0, result);
            Log.d(TAG, "Read buffer offset: " + byteOffset +
                    " count: " + byteCount + " read: " + result +
                    " val:\n" + temp);
            return result;
        }

        @Override
        public int read() throws IOException {
            int result = super.read();
            Log.d(TAG, "Read char: " + result);
            return result;
        }
    }


    static class DebugSocket extends Socket {

        protected final Socket mBase;

        DebugSocket(Socket base) {
            mBase = base;
        }

        @Override
        public synchronized void close() throws IOException {
            mBase.close();
        }

        @Override
        public InetAddress getInetAddress() {
            return mBase.getInetAddress();
        }

        private DebugInputStream mDebugInputStream;

        @Override
        public InputStream getInputStream() throws IOException {
            InputStream base = mBase.getInputStream();
            if (null == mDebugInputStream || mDebugInputStream.getBase() != base) {
                mDebugInputStream = new DebugInputStream(base);
            }
            return mDebugInputStream;
        }

        private DebugOutputStream mDebugOutputStream;

        @Override
        public OutputStream getOutputStream() throws IOException {
            OutputStream base = mBase.getOutputStream();
            if (null == mDebugOutputStream || mDebugOutputStream.getBase() != base) {
                mDebugOutputStream = new DebugOutputStream(base);
            }
            return mDebugOutputStream;
        }

        @Override
        public boolean getKeepAlive() throws SocketException {
            return mBase.getKeepAlive();
        }

        @Override
        public InetAddress getLocalAddress() {
            return mBase.getLocalAddress();
        }

        @Override
        public int getLocalPort() {
            return mBase.getLocalPort();
        }

        @Override
        public int getPort() {
            return mBase.getPort();
        }

        @Override
        public int getSoLinger() throws SocketException {
            return mBase.getSoLinger();
        }

        @Override
        public synchronized int getReceiveBufferSize() throws SocketException {
            return mBase.getReceiveBufferSize();
        }

        @Override
        public synchronized int getSendBufferSize() throws SocketException {
            return mBase.getSendBufferSize();
        }

        @Override
        public synchronized int getSoTimeout() throws SocketException {
            return mBase.getSoTimeout();
        }

        @Override
        public boolean getTcpNoDelay() throws SocketException {
            return mBase.getTcpNoDelay();
        }

        @Override
        public void setKeepAlive(boolean keepAlive) throws SocketException {
            mBase.setKeepAlive(keepAlive);
        }

        @Override
        public synchronized void setSendBufferSize(int size) throws SocketException {
            mBase.setSendBufferSize(size);
        }

        @Override
        public synchronized void setReceiveBufferSize(int size) throws SocketException {
            mBase.setReceiveBufferSize(size);
        }

        @Override
        public void setSoLinger(boolean on, int timeout) throws SocketException {
            mBase.setSoLinger(on, timeout);
        }

        @Override
        public synchronized void setSoTimeout(int timeout) throws SocketException {
            mBase.setSoTimeout(timeout);
        }

        @Override
        public void setTcpNoDelay(boolean on) throws SocketException {
            mBase.setTcpNoDelay(on);
        }

        @Override
        public String toString() {
            return mBase.toString();
        }

        @Override
        public void shutdownInput() throws IOException {
            mBase.shutdownInput();
        }

        @Override
        public void shutdownOutput() throws IOException {
            mBase.shutdownOutput();
        }

        @Override
        public SocketAddress getLocalSocketAddress() {
            return mBase.getLocalSocketAddress();
        }

        @Override
        public SocketAddress getRemoteSocketAddress() {
            return mBase.getRemoteSocketAddress();
        }

        @Override
        public boolean isBound() {
            return mBase.isBound();
        }

        @Override
        public boolean isConnected() {
            return mBase.isConnected();
        }

        @Override
        public boolean isClosed() {
            return mBase.isClosed();
        }

        @Override
        public void bind(SocketAddress localAddr) throws IOException {
            mBase.bind(localAddr);
        }

        @Override
        public void connect(SocketAddress remoteAddr) throws IOException {
            mBase.connect(remoteAddr);
        }

        @Override
        public void connect(SocketAddress remoteAddr, int timeout) throws IOException {
            mBase.connect(remoteAddr, timeout);
        }

        @Override
        public boolean isInputShutdown() {
            return mBase.isInputShutdown();
        }

        @Override
        public boolean isOutputShutdown() {
            return mBase.isOutputShutdown();
        }

        @Override
        public void setReuseAddress(boolean reuse) throws SocketException {
            mBase.setReuseAddress(reuse);
        }

        @Override
        public boolean getReuseAddress() throws SocketException {
            return mBase.getReuseAddress();
        }

        @Override
        public void setOOBInline(boolean oobinline) throws SocketException {
            mBase.setOOBInline(oobinline);
        }

        @Override
        public boolean getOOBInline() throws SocketException {
            return mBase.getOOBInline();
        }

        @Override
        public void setTrafficClass(int value) throws SocketException {
            mBase.setTrafficClass(value);
        }

        @Override
        public int getTrafficClass() throws SocketException {
            return mBase.getTrafficClass();
        }

        @Override
        public void sendUrgentData(int value) throws IOException {
            mBase.sendUrgentData(value);
        }

        @Override
        public SocketChannel getChannel() {
            return mBase.getChannel();
        }

        @Override
        public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
            mBase.setPerformancePreferences(connectionTime, latency, bandwidth);
        }
    }

    static class DebugSSLSocket extends SSLSocket {


        protected final SSLSocket mBase;

        DebugSSLSocket(SSLSocket base) {
            mBase = base;
        }

        @Override
        public String[] getEnabledProtocols() {
            return mBase.getEnabledProtocols();
        }

        @Override
        public void removeHandshakeCompletedListener(HandshakeCompletedListener listener) {
            mBase.removeHandshakeCompletedListener(listener);
        }

        @Override
        public boolean getEnableSessionCreation() {
            return mBase.getEnableSessionCreation();
        }

        @Override
        public SSLParameters getSSLParameters() {
            return mBase.getSSLParameters();
        }

        @Override
        public void setSSLParameters(SSLParameters p) {
            mBase.setSSLParameters(p);
        }

        @Override
        public void setEnabledProtocols(String[] protocols) {
            mBase.setEnabledProtocols(protocols);
        }

        @Override
        public void startHandshake() throws IOException {
            mBase.startHandshake();
        }

        @Override
        public void setWantClientAuth(boolean want) {
            mBase.setWantClientAuth(want);
        }

        @Override
        public boolean getWantClientAuth() {
            return mBase.getWantClientAuth();
        }

        @Override
        public boolean getUseClientMode() {
            return mBase.getUseClientMode();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return mBase.getSupportedCipherSuites();
        }

        @Override
        public SSLSession getSession() {
            return mBase.getSession();
        }

        @Override
        public String[] getEnabledCipherSuites() {
            return mBase.getEnabledCipherSuites();
        }

        @Override
        public void setEnableSessionCreation(boolean flag) {
            mBase.setEnableSessionCreation(flag);
        }

        @Override
        public String[] getSupportedProtocols() {
            return mBase.getSupportedProtocols();
        }

        @Override
        public void addHandshakeCompletedListener(HandshakeCompletedListener listener) {
            mBase.addHandshakeCompletedListener(listener);
        }

        @Override
        public boolean getNeedClientAuth() {
            return mBase.getNeedClientAuth();
        }

        @Override
        public void setNeedClientAuth(boolean need) {
            mBase.setNeedClientAuth(need);
        }

        @Override
        public void setUseClientMode(boolean mode) {
            mBase.setUseClientMode(mode);
        }

        @Override
        public synchronized void close() throws IOException {
            mBase.close();
        }

        @Override
        public InetAddress getInetAddress() {
            return mBase.getInetAddress();
        }

        private DebugInputStream mDebugInputStream;

        @Override
        public InputStream getInputStream() throws IOException {
            InputStream base = mBase.getInputStream();
            if (null == mDebugInputStream || mDebugInputStream.getBase() != base) {
                mDebugInputStream = new DebugInputStream(base);
            }
            return mDebugInputStream;
        }

        private DebugOutputStream mDebugOutputStream;

        @Override
        public OutputStream getOutputStream() throws IOException {
            OutputStream base = mBase.getOutputStream();
            if (null == mDebugOutputStream || mDebugOutputStream.getBase() != base) {
                mDebugOutputStream = new DebugOutputStream(base);
            }
            return mDebugOutputStream;
        }

        @Override
        public boolean getKeepAlive() throws SocketException {
            return mBase.getKeepAlive();
        }

        @Override
        public InetAddress getLocalAddress() {
            return mBase.getLocalAddress();
        }

        @Override
        public int getLocalPort() {
            return mBase.getLocalPort();
        }

        @Override
        public int getPort() {
            return mBase.getPort();
        }

        @Override
        public int getSoLinger() throws SocketException {
            return mBase.getSoLinger();
        }

        @Override
        public synchronized int getReceiveBufferSize() throws SocketException {
            return mBase.getReceiveBufferSize();
        }

        @Override
        public synchronized int getSendBufferSize() throws SocketException {
            return mBase.getSendBufferSize();
        }

        @Override
        public synchronized int getSoTimeout() throws SocketException {
            return mBase.getSoTimeout();
        }

        @Override
        public boolean getTcpNoDelay() throws SocketException {
            return mBase.getTcpNoDelay();
        }

        @Override
        public void setKeepAlive(boolean keepAlive) throws SocketException {
            mBase.setKeepAlive(keepAlive);
        }

        @Override
        public synchronized void setSendBufferSize(int size) throws SocketException {
            mBase.setSendBufferSize(size);
        }

        @Override
        public synchronized void setReceiveBufferSize(int size) throws SocketException {
            mBase.setReceiveBufferSize(size);
        }

        @Override
        public void setSoLinger(boolean on, int timeout) throws SocketException {
            mBase.setSoLinger(on, timeout);
        }

        @Override
        public synchronized void setSoTimeout(int timeout) throws SocketException {
            mBase.setSoTimeout(timeout);
        }

        @Override
        public void setTcpNoDelay(boolean on) throws SocketException {
            mBase.setTcpNoDelay(on);
        }

        @Override
        public String toString() {
            return mBase.toString();
        }

        @Override
        public void shutdownInput() throws IOException {
            mBase.shutdownInput();
        }

        @Override
        public void shutdownOutput() throws IOException {
            mBase.shutdownOutput();
        }

        @Override
        public void setEnabledCipherSuites(String[] suites) {
            mBase.setEnabledCipherSuites(suites);
        }

        @Override
        public SocketAddress getLocalSocketAddress() {
            return mBase.getLocalSocketAddress();
        }

        @Override
        public SocketAddress getRemoteSocketAddress() {
            return mBase.getRemoteSocketAddress();
        }

        @Override
        public boolean isBound() {
            return mBase.isBound();
        }

        @Override
        public boolean isConnected() {
            return mBase.isConnected();
        }

        @Override
        public boolean isClosed() {
            return mBase.isClosed();
        }

        @Override
        public void bind(SocketAddress localAddr) throws IOException {
            mBase.bind(localAddr);
        }

        @Override
        public void connect(SocketAddress remoteAddr) throws IOException {
            mBase.connect(remoteAddr);
        }

        @Override
        public void connect(SocketAddress remoteAddr, int timeout) throws IOException {
            mBase.connect(remoteAddr, timeout);
        }

        @Override
        public boolean isInputShutdown() {
            return mBase.isInputShutdown();
        }

        @Override
        public boolean isOutputShutdown() {
            return mBase.isOutputShutdown();
        }

        @Override
        public void setReuseAddress(boolean reuse) throws SocketException {
            mBase.setReuseAddress(reuse);
        }

        @Override
        public boolean getReuseAddress() throws SocketException {
            return mBase.getReuseAddress();
        }

        @Override
        public void setOOBInline(boolean oobinline) throws SocketException {
            mBase.setOOBInline(oobinline);
        }

        @Override
        public boolean getOOBInline() throws SocketException {
            return mBase.getOOBInline();
        }

        @Override
        public void setTrafficClass(int value) throws SocketException {
            mBase.setTrafficClass(value);
        }

        @Override
        public int getTrafficClass() throws SocketException {
            return mBase.getTrafficClass();
        }

        @Override
        public void sendUrgentData(int value) throws IOException {
            mBase.sendUrgentData(value);
        }

        @Override
        public SocketChannel getChannel() {
            return mBase.getChannel();
        }

        @Override
        public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
            mBase.setPerformancePreferences(connectionTime, latency, bandwidth);
        }


    }

    abstract static class DebugSSLSocketFactory extends SSLSocketFactory {

        abstract protected SSLSocketFactory getBase();

        DebugSSLSocketFactory() {
            getBase();
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return getBase().getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return getBase().getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            Socket result = getBase().createSocket(s, host, port, autoClose);
            return wrap(result);
        }

        private Socket wrap(Socket s) {
            if (false) {
                return s;
            }
            if (s instanceof SSLSocket) {
                return new Util.DebugSSLSocket((SSLSocket) s);
            }
            return new Util.DebugSocket(s);

        }

        @Override
        public Socket createSocket() throws IOException {
            Socket result = getBase().createSocket();
            return wrap(result);
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
            Socket result = getBase().createSocket(host, port);
            return wrap(result);
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
            Socket result = getBase().createSocket(host, port, localHost, localPort);
            return wrap(result);
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            Socket result = getBase().createSocket(host, port);
            return wrap(result);
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            Socket result = getBase().createSocket(address, port, localAddress, localPort);
            return wrap(result);
        }
    }


}
