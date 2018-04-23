package com.sunmi.printerservice.entity;

public class BlackLabelEntity {
	private int print_mode;
	private double cutter_location;
	private int print_spec;

	public double getCutter_location() {
		return cutter_location;
	}
	public void setCutter_location(double cutter_location) {
		this.cutter_location = cutter_location;
	}
	public int getPrint_mode() {
		return print_mode;
	}
	public void setPrint_mode(int print_mode) {
		this.print_mode = print_mode;
	}
	public int getPrint_spec() {
		return print_spec;
	}
	public void setPrint_spec(int print_spec) {
		this.print_spec = print_spec;
	}
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "BlackLabel:[cutter_location="
				+ cutter_location + ",print_mode="+print_mode
				+ "print_spec=" + print_spec + "]";
	}
}
