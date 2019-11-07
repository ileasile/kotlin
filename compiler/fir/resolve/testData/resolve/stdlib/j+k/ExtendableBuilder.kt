// FILE: ExtendableBuilder.java

public abstract class ExtendableBuilder<MessageType extends ExtendableMessage<MessageType>, BuilderType extends ExtendableBuilder<MessageType, BuilderType>>
        extends Builder<MessageType, BuilderType>
        implements ExtendableMessageOrBuilder<MessageType>
{
    public final <Type> Type getExtension(GeneratedExtension<MessageType, Type> extension) {}
}

// FILE: ExtendableMessage.java

public abstract class ExtendableMessage<MessageType extends ExtendableMessage<MessageType>> implements ExtendableMessageOrBuilder<MessageType> {
    public final <Type> Type getExtension(GeneratedExtension<MessageType, Type> extension) {}
}

// FILE: ExtendableMessageOrBuilder.java

public interface ExtendableMessageOrBuilder<MessageType extends ExtendableMessage> extends MessageLiteOrBuilder {
    <Type> Type getExtension(GeneratedMessageLite.GeneratedExtension<MessageType, Type> var1) {}
}

// FILE: GeneratedExtension.java

public class GeneratedExtension<ContainingType extends MessageLite, Type> {}

// FILE: MessageLite.java

public interface MessageLite extends MessageLiteOrBuilder {}

// FILE: MessageLiteOrBuilder.java

public interface MessageLiteOrBuilder {}

// FILE: test.kt

fun <M : ExtendableMessage<M>, T> ExtendableMessage<M>.getExtensionOrNull(
    extension: GeneratedExtension<M, T>
): T? = getExtension(extension)
