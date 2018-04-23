package com.longcheer.spijni;

public class SpiJni {
	private static String device = "/dev/spidev0.0";
	private static String device3 = "/sys/printer_pin/printer/busy1";
	private static String device2 = "/sys/printer_pin/printer/block2";
	private static String device1 = "/sys/printer_pin/printer/block1";
	private static String device4 = "/sys/printer_pin/printer/busy2";

	private static String device5 = "/sys/class/gpio/gpio938/value";
	private static String device6 = "/sys/cash_drawer/kickout";


	public int fd_spi;
	public int fd_block1;
	public int fd_block2;
	public int fd_rsest;
	public int fd_box;


	public native int open(String s, int type);

	public native int close(int fd);

	public native int spi_transfer(int fd, int length, byte[] tx, byte[] rx);

	public native int spi_setoption(int fd, String opt);

	public native int gpio_read(int fd);

	public native int gpio_write(int fd, int l);

	public SpiJni() {
		fd_spi = open(device, 0);
		fd_block1 = open(device2, 1);
		fd_block2 = open(device1, 1);
		fd_rsest = open(device5, 2);
		fd_box = open(device6, 2);
	}

	public void close() {
		close(fd_spi);
		close(fd_block1);
		close(fd_block2);
		close(fd_rsest);
		close(fd_box);;
	}


	public int readbusy1(){
		int fd_busy1 = open(device4, 1);
		int res = gpio_read(fd_busy1);
		close(fd_busy1);
		return res;
	}

	public int readbusy2(){
		int fd_busy2 = open(device3, 1);
		int res = gpio_read(fd_busy2);
		close(fd_busy2);
		return res;
	}

	public void readblock1(){
		gpio_read(fd_block1);
	}

	public void readblock2(){
		gpio_read(fd_block2);
	}

	static {
		System.loadLibrary("Testlib");
	}

}