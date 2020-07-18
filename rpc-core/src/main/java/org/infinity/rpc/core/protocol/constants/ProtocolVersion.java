package org.infinity.rpc.core.protocol.constants;

public enum ProtocolVersion {
    VERSION_1((byte) 1, 16);

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
