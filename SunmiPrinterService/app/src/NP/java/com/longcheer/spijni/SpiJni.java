package com.longcheer.spijni;


public class SpiJni {
	private static String device6 = "/sys/cash_drawer/kickstate";

	public int fd_spi;
	public int fd_rsest;
	public int fd_box;

	public native int open(String s, int type);

	public native int close(int fd);

	public native int spi_transfer(int fd, int length, byte[] tx, byte[] rx);

	public native int spi_setoption(int fd, String opt);

	public native int gpio_read(int fd);

	public native int gpio_write(int fd, int l);

	public SpiJni() {
		fd_box = open(device6, 2);
	}

	public int readbusy1(){
		return -1;
	}

	public int readbusy2(){
		return -1;
	}

	public void readblock1(){

	}

	public void readblock2(){

	}

	public void close() {
		close(fd_box);
	}

	static {
		System.loadLibrary("Testlib");
	}

}