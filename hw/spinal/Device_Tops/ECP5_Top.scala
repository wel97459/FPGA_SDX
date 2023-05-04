package Device_Tops

import spinal.core._
import spinal.lib._
import MySpinalHardware._
import DSPs_SpinalHDL._
import FPGA_SDX._

case class ecp5_pll() extends BlackBox {
    val io = new Bundle {
        val clkin = in Bool()
        val clkout0 = out Bool()
        val locked = out Bool()
    }
noIoPrefix()
}


class Top_ECP5 extends Component
{
        val io = new Bundle{
        val reset_ = in Bool()
        val clk_25Mhz = in Bool() //12Mhz CLK
        val si_14Mhz = in Bool()

        val led_red = out Bool()
        val sound_dac_p = out Bool()
        val sound_dac_n = out Bool()

        val bclk = in Bool()
        val lrclk = in Bool()
        val sdata = in Bool()

    }
    noIoPrefix()

    val clk25Domain = ClockDomain.internal(name = "Core25",  frequency = FixedFrequency(25.0000 MHz))
    val clk30Domain = ClockDomain.internal(name = "Core30",  frequency = FixedFrequency(20.0000 MHz))

    //Define PLL
    val PLL = new ecp5_pll()
    //Setup signals of PLL
    PLL.io.clkin := io.clk_25Mhz


    clk25Domain.clock := io.clk_25Mhz
    clk25Domain.reset := !io.reset_

    clk30Domain.clock := PLL.io.clkout0
    clk30Domain.reset := !io.reset_

    val Core25 = new ClockingArea(clk25Domain) {
        val pwm = new LedGlow(24)
        io.led_red := !(pwm.io.led)
    }

    val Core30 = new ClockingArea(clk30Domain) {
        val top = new FPGA_SDX_TOP()
        top.io.bclk := io.bclk
        top.io.lrclk := io.lrclk
        top.io.sdata := io.sdata
        io.sound_dac_p := top.io.sound_dac_p
        io.sound_dac_n := top.io.sound_dac_n
    }
}

object Top_ECP5_CLI5v6_Verilog extends App {
    Config_ECP5_CLI5v6.spinal.generateVerilog(new Top_ECP5())
}
