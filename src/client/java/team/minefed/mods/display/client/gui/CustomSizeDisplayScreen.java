package team.minefed.mods.display.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import team.minefed.mods.display.blocks.CustomSizeDisplayBlock;
import team.minefed.mods.display.client.network.ClientDisplayModMessages;
import team.minefed.mods.display.network.UpdateCustomDisplayPacket;

public class CustomSizeDisplayScreen extends Screen {

    private final BlockPos pos;
    private TextFieldWidget urlField;
    private TextFieldWidget widthField;
    private TextFieldWidget heightField;
    private String currentUrl;
    private int currentWidth;
    private int currentHeight;

    public CustomSizeDisplayScreen(BlockPos pos, String currentUrl, int currentWidth, int currentHeight) {
        super(Text.of("Custom Size Display Settings"));
        this.pos = pos;
        this.currentUrl = currentUrl;
        this.currentWidth = currentWidth;
        this.currentHeight = currentHeight;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // URL field
        this.urlField = new TextFieldWidget(this.textRenderer, centerX - 150, centerY - 50, 300, 20, Text.of(""));
        this.urlField.setMaxLength(256);
        this.urlField.setText(this.currentUrl);
        this.urlField.setPlaceholder(Text.translatable("gui.custom_size_display.url"));
        this.addDrawableChild(this.urlField);

        // Width field
        this.widthField = new TextFieldWidget(this.textRenderer, centerX - 150, centerY - 20, 140, 20, Text.of(""));
        this.widthField.setMaxLength(2);
        this.widthField.setText(String.valueOf(this.currentWidth));
        this.widthField.setPlaceholder(Text.translatable("gui.custom_size_display.width"));
        this.addDrawableChild(this.widthField);

        // Height field
        this.heightField = new TextFieldWidget(this.textRenderer, centerX + 10, centerY - 20, 140, 20, Text.of(""));
        this.heightField.setMaxLength(2);
        this.heightField.setText(String.valueOf(this.currentHeight));
        this.heightField.setPlaceholder(Text.translatable("gui.custom_size_display.height"));
        this.addDrawableChild(this.heightField);

        // Save button
        this.addDrawableChild(ButtonWidget.builder(Text.of("Save"), button -> {
            int newWidth = parseIntSafe(this.widthField.getText(), this.currentWidth);
            int newHeight = parseIntSafe(this.heightField.getText(), this.currentHeight);

            // Clamp values
            newWidth = Math.max(CustomSizeDisplayBlock.MIN_SIZE, Math.min(CustomSizeDisplayBlock.MAX_SIZE, newWidth));
            newHeight = Math.max(CustomSizeDisplayBlock.MIN_SIZE, Math.min(CustomSizeDisplayBlock.MAX_SIZE, newHeight));

            ClientDisplayModMessages.sendToServer(new UpdateCustomDisplayPacket(
                    this.pos, this.urlField.getText(), newWidth, newHeight));
            this.close();
        }).dimensions(centerX - 100, centerY + 20, 200, 20).build());
    }

    private int parseIntSafe(String text, int defaultValue) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Draw labels
        context.drawTextWithShadow(this.textRenderer, Text.of("URL:"), centerX - 150, centerY - 60, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.of("Width:"), centerX - 150, centerY - 30, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.of("Height:"), centerX + 10, centerY - 30, 0xFFFFFF);

        // Draw size limits hint
        String hint = String.format("Size: %d-%d blocks", CustomSizeDisplayBlock.MIN_SIZE,
                CustomSizeDisplayBlock.MAX_SIZE);
        context.drawTextWithShadow(this.textRenderer, Text.of(hint), centerX - 150, centerY + 5, 0xAAAAAA);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
