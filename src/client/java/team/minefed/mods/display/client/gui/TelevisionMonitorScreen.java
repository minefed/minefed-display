package team.minefed.mods.display.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import team.minefed.mods.display.client.network.ClientDisplayModMessages;
import team.minefed.mods.display.network.UpdateUrlPacket;

public class TelevisionMonitorScreen extends Screen {

    private final BlockPos pos;
    private TextFieldWidget urlField;
    private String currentUrl;

    public TelevisionMonitorScreen(BlockPos pos, String currentUrl) {
        super(Text.of("Set Display URL"));
        this.pos = pos;
        this.currentUrl = currentUrl;
    }

    @Override
    protected void init() {
        super.init();
        this.urlField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, this.height / 2 - 10, 200, 20, Text.of(""));
        this.urlField.setText(this.currentUrl);
        this.urlField.setPlaceholder(Text.translatable("gui.television_monitor.url"));
        this.urlField.setMaxLength(256);
        this.addDrawableChild(this.urlField);

        this.addDrawableChild(ButtonWidget.builder(Text.of("Save"), button -> {
            ClientDisplayModMessages.sendToServer(new UpdateUrlPacket(this.pos, this.urlField.getText()));
            this.close();
        }).dimensions(this.width / 2 - 100, this.height / 2 + 20, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
    }
} 