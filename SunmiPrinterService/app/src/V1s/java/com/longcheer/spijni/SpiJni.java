package com.longcheer.spijni;

public class SpiJni {
	private static String device = "/dev/print_spi1.1";
	private static String device4 = "/sys/class/spi-print/print_spi1.1/device/gpio_spi_busy_cts";
	private static String device1= "/sys/class/spi-print/print_spi1.1/device/gpio_spi_block_cts";
	private static String device2= "/sys/class/spi-print/print_spi1.1/device/gpio_spi_block_rts";
	private static String device3= "/sys/class/spi-print/print_spi1.1/device/gpio_spi_busy_rts";

	private static String device5 = "/sys/class/spi-print/print_spi1.1/device/gpio_rst_mcu";
	private static String device6 = "/sys/class/spi-print/print_spi1.1/device/gpio_mcu_sleep_rx";
	private static String device7 = "/sys/class/spi-print/print_spi1.1/device/gpio_mcu_resume_tx";
	private static String device8 = "/sys/class/spi-print/print_spi1.1/device/gpio_oe_level_status";

	public int fd_spi;
	public int fd_rsest;
	public int fd_sleep;
	public int fd_resume;
	public int fd_power;

	public native int open(String s, int type);

	public native int close(int fd);

	public native int spi_transfer(int fd, int length, byte[] tx, byte[] rx);

	public native int spi_setoption(int fd, String opt);

	public native int gpio_read(int fd);

	public native int gpio_write(int fd, int l);

	public SpiJni() {
		fd_spi = open(device, 0);
		fd_rsest = open(device5, 2);
		fd_resume = open(device6, 2);
		fd_sleep = open(device7, 2);
		fd_power = open(device8, 2);
	}

	public void close() {
		close(fd_spi);
		close(fd_rsest);
		close(fd_sleep);
		close(fd_resume);
		close(fd_power);
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
		int fd_block1 = open(device1, 1);
		gpio_read(fd_block1);
		close(fd_block1);
	}

	public void readblock2(){
		int fd_block2 = open(device2, 1);
		gpio_read(fd_block2);
		close(fd_block2);
	}

	static {
		System.loadLibrary("Testlib");
	}

}