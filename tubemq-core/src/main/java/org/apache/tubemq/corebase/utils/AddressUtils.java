/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tubemq.corebase.utils;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import org.apache.tubemq.corebase.exception.AddressException;
import org.jboss.netty.channel.Channel;

public class AddressUtils {

    private static String localIPAddress = null;

    public static synchronized String getLocalAddress() throws Exception {
        if (TStringUtils.isBlank(localIPAddress)) {
            throw new Exception("Local IP is blank, Please initial Client's Configure first!");
        }
        return localIPAddress;
    }

    @Deprecated
    public static boolean validLocalIp(String currLocalHost) {
        if (TStringUtils.isNotEmpty(localIPAddress)
                && localIPAddress.equals(currLocalHost)) {
            return true;
        }

        Enumeration<NetworkInterface> allInterface = listNetworkInterface();

        return checkValidIp(allInterface, currLocalHost);
    }

    private static boolean checkValidIp(Enumeration<NetworkInterface> allInterface, String currLocalHost) {
        String localIp;
        try {
            while (allInterface.hasMoreElements()) {
                NetworkInterface oneInterface = allInterface.nextElement();
                if (oneInterface == null
                        || oneInterface.isLoopback()
                        || !oneInterface.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> allAddress = oneInterface.getInetAddresses();
                while (allAddress.hasMoreElements()) {
                    InetAddress oneAddress = allAddress.nextElement();
                    localIp = oneAddress.getHostAddress();
                    if (TStringUtils.isBlank(localIp)
                            || "127.0.0.1".equals(localIp)) {
                        continue;
                    }
                    if (localIp.equals(currLocalHost)) {
                        localIPAddress = localIp;
                        return true;
                    }
                }
            }
        } catch (SocketException e) {
            throw new AddressException("error get local ip, ex {}", e);
        }
        throw new AddressException(new StringBuilder(256).append("Illegal parameter: not found the ip(")
                .append(currLocalHost).append(") in local networkInterfaces!").toString());
    }

    public static int ipToInt(String ipAddr) {
        try {
            return bytesToInt(toBytes(ipAddr));
        } catch (Exception e) {
            throw new IllegalArgumentException(ipAddr + " is invalid IP");
        }
    }

    public static String intToIp(int ipInt) {
        return new StringBuilder(128).append(((ipInt >> 24) & 0xff)).append('.')
                .append((ipInt >> 16) & 0xff).append('.').append((ipInt >> 8) & 0xff).append('.')
                .append((ipInt & 0xff)).toString();
    }

    public static byte[] toBytes(String ipAddr) {
        byte[] ret = new byte[4];
        try {
            String[] ipArr = ipAddr.split("\\.");
            ret[0] = (byte) (Integer.parseInt(ipArr[0]) & 0xFF);
            ret[1] = (byte) (Integer.parseInt(ipArr[1]) & 0xFF);
            ret[2] = (byte) (Integer.parseInt(ipArr[2]) & 0xFF);
            ret[3] = (byte) (Integer.parseInt(ipArr[3]) & 0xFF);
            return ret;
        } catch (Exception e) {
            throw new IllegalArgumentException(ipAddr + " is invalid IP");
        }
    }

    public static int bytesToInt(byte[] bytes) {
        int addr = bytes[3] & 0xFF;
        addr |= ((bytes[2] << 8) & 0xFF00);
        addr |= ((bytes[1] << 16) & 0xFF0000);
        addr |= ((bytes[0] << 24) & 0xFF000000);
        return addr;
    }

    public static String getRemoteAddressIP(Channel channel) {
        String strRemoteIP = null;
        if (channel == null) {
            return strRemoteIP;
        }
        SocketAddress remoteSocketAddress = channel.getRemoteAddress();
        if (null != remoteSocketAddress) {
            strRemoteIP = remoteSocketAddress.toString();
            try {
                strRemoteIP = strRemoteIP.substring(1, strRemoteIP.indexOf(':'));
            } catch (Exception ee) {
                return strRemoteIP;
            }
        }
        return strRemoteIP;
    }

    public static Enumeration<NetworkInterface> listNetworkInterface() {
        try {
            return NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            throw new AddressException(e);
        }
    }

    public static String getIPV4LocalAddress() {
        String tmpAdress = null;
        try {
            Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
            if (enumeration == null) {
                throw new AddressException("Get NetworkInterfaces is null");
            }
            while (enumeration.hasMoreElements()) {
                try {
                    tmpAdress = getValidIPV4Address(enumeration.nextElement());
                    if (tmpAdress != null) {
                        break;
                    }
                } catch (Throwable e) {
                    //
                }
            }
            if (tmpAdress == null) {
                tmpAdress = InetAddress.getLocalHost().getHostAddress();
            }
            if (tmpAdress != null) {
                localIPAddress = tmpAdress;
                return tmpAdress;
            }
        } catch (SocketException | UnknownHostException e) {
            throw new AddressException("Call getIPV4LocalAddress throw exception", e);
        }
        throw new AddressException(new StringBuilder(256)
            .append("Illegal parameter: not found the default ip")
            .append(" in local networkInterfaces!").toString());
    }

    public static String getIPV4LocalAddress(String defEthName) {
        boolean foundNetInter = false;
        String tmpAdress = null;
        try {
            Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
            if (enumeration == null) {
                throw new AddressException("Get NetworkInterfaces is null");
            }
            while (enumeration.hasMoreElements()) {
                NetworkInterface oneInterface = enumeration.nextElement();
                if (oneInterface == null
                    || oneInterface.isLoopback()
                    || !oneInterface.isUp()
                    || !defEthName.equalsIgnoreCase(oneInterface.getName())) {
                    continue;
                }
                foundNetInter = true;
                try {
                    tmpAdress = getValidIPV4Address(oneInterface);
                    if (tmpAdress != null) {
                        localIPAddress = tmpAdress;
                        return tmpAdress;
                    }
                } catch (Throwable e) {
                    //
                }
            }
        } catch (Throwable e) {
            throw new AddressException("Call getDefNetworkAddress throw exception", e);
        }
        if (foundNetInter) {
            throw new AddressException(new StringBuilder(256)
                .append("Illegal parameter: not found valid ip")
                .append(" in networkInterfaces ").append(defEthName).toString());
        } else {
            throw new AddressException(new StringBuilder(256)
                .append("Illegal parameter: ").append(defEthName)
                .append(" does not exist or is not in a valid state!").toString());
        }
    }

    public static String getValidIPV4Address(NetworkInterface networkInterface) {
        try {
            if (networkInterface == null ||
                !networkInterface.isUp() ||
                networkInterface.isLoopback() ||
                "docker0".equals(networkInterface.getName())) {
                return null;
            }
            Enumeration<InetAddress> addrs = networkInterface.getInetAddresses();
            while (addrs.hasMoreElements()) {
                InetAddress address = addrs.nextElement();
                if (address == null ||
                    address.isLoopbackAddress() ||
                    address instanceof Inet6Address) {
                    continue;
                }
                String localIP = address.getHostAddress();
                if (TStringUtils.isEmpty(localIP) || localIP.startsWith("127.0")) {
                    continue;
                }
                return localIP;
            }
            return null;
        } catch (Throwable e) {
            throw new AddressException(new StringBuilder(256)
                .append("Illegal parameter: ").append("unable to obtain valid IP from network card ")
                .append(networkInterface).toString(), e);
        }
    }

}
