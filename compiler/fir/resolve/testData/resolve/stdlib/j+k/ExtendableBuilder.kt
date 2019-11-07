// FILE: GeneratedMessageLite.java

public class GeneratedMessageLite {
    public abstract static class ExtendableBuilder<MessageType extends ExtendableMessage<MessageType>, BuilderType extends ExtendableBuilder<MessageType, BuilderType>>
        extends Builder<MessageType, BuilderType>
        implements ExtendableMessageOrBuilder<MessageType>
    {
        public final <Type> Type getExtension(final GeneratedExtension<MessageType, Type> extension) {}
    }

    public abstract static class ExtendableMessage<MessageType extends ExtendableMessage<MessageType>> implements ExtendableMessageOrBuilder<MessageType>
    {
        public final <Type> Type getExtension(final GeneratedExtension<MessageType, Type> extension) {}
    }

    public interface ExtendableMessageOrBuilder<MessageType extends ExtendableMessage> extends MessageLiteOrBuilder
    {
        <Type> Type getExtension (GeneratedMessageLite.GeneratedExtension<MessageType, Type> var1) {}
    }
}

// FILE: GeneratedExtension.java

public class GeneratedExtension<ContainingType extends MessageLite, Type> {}

// FILE: MessageLite.java

public interface MessageLite extends MessageLiteOrBuilder {}

// FILE: MessageLiteOrBuilder.java

public interface MessageLiteOrBuilder {}

// FILE: test.kt

fun <M : GeneratedMessageLite.ExtendableMessage<M>, T> GeneratedMessageLite.ExtendableMessage<M>.getExtensionOrNull(
    extension: GeneratedExtension<M, T>
): T? = getExtension(extension)
