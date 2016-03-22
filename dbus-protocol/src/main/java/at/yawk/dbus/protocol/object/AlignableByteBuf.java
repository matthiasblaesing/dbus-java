package at.yawk.dbus.protocol.object;

import io.netty.buffer.ByteBuf;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Delegate;

/**
 * @author yawkat
 */
@ToString
@EqualsAndHashCode(callSuper = false)
public class AlignableByteBuf extends ByteBuf {
    @Delegate private final ByteBuf buf;
    private final int messageOffset;
    private final int baseAlignment;

    // visible for testing
    public AlignableByteBuf(ByteBuf buf, int messageOffset, int baseAlignment) {
        this.buf = buf;
        this.messageOffset = messageOffset;
        this.baseAlignment = baseAlignment;
    }

    public static AlignableByteBuf decoding(ByteBuf wrapping) {
        return decoding(wrapping, 1 << 30);
    }

    public static AlignableByteBuf decoding(ByteBuf wrapping, int baseAlignment) {
        return new AlignableByteBuf(wrapping, -wrapping.readerIndex(), baseAlignment);
    }

    /**
     * Create a new {@link AlignableByteBuf} from the given message buffer. The message must start at {@code
     * buffer[0]}.
     */
    public static AlignableByteBuf encoding(ByteBuf wrapping) {
        return new AlignableByteBuf(wrapping, 0, 1 << 30);
    }

    /**
     * Create a new {@link AlignableByteBuf} from a buffer that is known to be aligned to a block boundary with the
     * given block size.
     *
     * @param existingAlignment the alignment of the given buffer.
     */
    public static AlignableByteBuf fromAlignedBuffer(ByteBuf buffer, int existingAlignment) {
        return new AlignableByteBuf(buffer, 0, existingAlignment);
    }

    private int calculateAlignmentOffset(int position, int alignment) {
        return (alignment - ((this.messageOffset + position) % alignment)) % alignment;
    }

    private boolean canAlign(int alignment) {
        return (baseAlignment % alignment) == 0;
    }

    private void checkAlign(int alignment) {
        if (!canAlign(alignment)) {
            throw new IllegalArgumentException(
                    "Cannot align to boundary " + alignment + ": base boundary is " + baseAlignment);
        }
    }

    public boolean canAlignRead(int alignment) {
        if (!canAlign(alignment)) { return false; }
        int toPad = calculateAlignmentOffset(readerIndex(), alignment);
        return readableBytes() >= toPad;
    }

    public void alignRead(int alignment) {
        checkAlign(alignment);
        int toPad = calculateAlignmentOffset(readerIndex(), alignment);
        for (int i = 0; i < toPad; i++) {
            if (readByte() != 0) {
                throw new DeserializerException("Non-null byte in alignment padding");
            }
        }
    }

    public boolean canAlignWrite(int alignment) {
        if (!canAlign(alignment)) { return false; }
        int toPad = calculateAlignmentOffset(writerIndex(), alignment);
        return writableBytes() >= toPad;
    }

    public void alignWrite(int alignment) {
        checkAlign(alignment);
        int toPad = calculateAlignmentOffset(writerIndex(), alignment);
        for (int i = 0; i < toPad; i++) {
            writeByte(0);
        }
    }
}
