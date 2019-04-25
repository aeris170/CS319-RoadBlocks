package com.pmnm.risk.ui;

import java.awt.Color;
import java.awt.Font;

import com.doa.engine.graphics.DoaGraphicsContext;
import com.doa.engine.graphics.DoaSprite;
import com.doa.maths.DoaVectorF;
import com.doa.ui.button.DoaImageButton;

public class TextImageButton extends DoaImageButton {

	private static final long serialVersionUID = -3498878656892515070L;

	protected String text;
	protected Color textColor;
	protected Color hoverTextColor;

	public TextImageButton(DoaVectorF position, Integer width, Integer height, DoaSprite idleImage, DoaSprite hoverImage, String text, Color textColor,
	        Color hoverTextColor) {
		super(position, width, height, idleImage, hoverImage);
		this.text = text;
		this.textColor = textColor;
		this.hoverTextColor = hoverTextColor;
	}

	public void setText(String s) {
		text = s;
	}

	@Override
	public void render(DoaGraphicsContext g) {
		super.render(g);
		g.setFont(UIInit.UI_FONT.deriveFont(Font.PLAIN, 36f));
		g.setColor(textColor);
		if (hover) {
			g.setColor(hoverTextColor);
		}
		g.drawString(text, position.x + 20, position.y + height - 17);
	}
}