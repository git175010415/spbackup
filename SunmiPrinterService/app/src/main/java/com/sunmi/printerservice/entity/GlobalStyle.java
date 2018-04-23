package com.sunmi.printerservice.entity;


public class GlobalStyle {
	private int font_height;
	private int font_width;
	private int font_weight;
	private int inverse_white;
	private int underline;
	private int row_height;
	private double row_space;

	public int getFont_height() {
		return font_height;
	}
	public void setFont_height(int font_height) {
		this.font_height = font_height;
	}
	public int getFont_width() {
		return font_width;
	}
	public void setFont_width(int font_width) {
		this.font_width = font_width;
	}
	public int getFont_weight() {
		return font_weight;
	}
	public void setFont_weight(int font_weight) {
		this.font_weight = font_weight;
	}
	public int getInverse_white() {
		return inverse_white;
	}
	public void setInverse_white(int inverse_white) {
		this.inverse_white = inverse_white;
	}
	public double getRow_space() {
		return row_space;
	}
	public void setRow_space(double row_space) {
		this.row_space = row_space;
	}
	public int getUnderline() {
		return underline;
	}
	public void setUnderline(int underline) {
		this.underline = underline;
	}
	public int getRow_height() {
		return row_height;
	}
	public void setRow_height(int row_height) {
		this.row_height = row_height;
	}


	@Override
	public String toString() {
		return "GlobalSettings [font_height=" + font_height + ", font_width=" + font_width + ", font_weight="
				+ font_weight + ", inverse_white=" + inverse_white + ", row_space=" + row_space + ", underline="
				+ underline + ", row_height=" + row_height + "]";
	}
	
}
