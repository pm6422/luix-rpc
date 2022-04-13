package com.luixtech.rpc.core.protocol.constants;

public enum ProtocolVersion {
    VERSION_1((byte) 1, 16),
    VERSION_1_Compress((byte) 2, 16),
    VERSION_2((byte) 3, 13);

    private byte version;
    private int  headerLength;

    ProtocolVersion(byte version, int headerLength) {
        this.version = version;
        this.headerLength = headerLength;
    }

    public byte getVersion() {
        return version;
    }

    public int getHeaderLength() {
        return headerLength;
    }
}
