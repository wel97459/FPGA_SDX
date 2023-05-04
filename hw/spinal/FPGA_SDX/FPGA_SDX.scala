package FPGA_SDX

import spinal.core._
import spinal.core.sim._
import scala.util.control.Breaks
import scala.collection.mutable.ArrayBuffer
import DSPs_SpinalHDL._
import MySpinalHardware._


class FPGA_SDX_TOP extends Component
{
    val io = new Bundle {
        val bclk = in Bool()
        val lrclk = in Bool()
        val sdata = in Bool()

        val sound_dac_p = out Bool()
        val sound_dac_n = out Bool()
    }

    // val sdm = new Sigma_Delta_Modulator()

    // val c = Reg(UInt(2 bits))
    // c := c + 1

    // sdm.io.i_data := 0
    // when(c === 0){
    //     sdm.io.i_data := 127
    // }elsewhen(c === 1){
    //     sdm.io.i_data := 0
    // }elsewhen(c === 2){
    //     sdm.io.i_data := -127
    // }elsewhen(c === 3){
    //     sdm.io.i_data := 0
    // }

    val i2s_decoder = new I2S_In()
    val dac = new Delta_Sigma_DAC_SOrder(31)
    val cic = new CIC_Interpolation(32,3,40,1)
    val downs = new DownSampler(32, 40)
    val audio = Reg(UInt(32 bits)) init(0)    

    i2s_decoder.io.i_lrclk := io.lrclk
    i2s_decoder.io.i_sclk := io.bclk
    i2s_decoder.io.i_sdin := io.sdata

    i2s_decoder.io.i_ready := False
    when(i2s_decoder.io.o_valid){
        i2s_decoder.io.i_ready := True
        audio := i2s_decoder.io.o_audio_l
    }

    // when(i2s_decoder.io.bitLength === B"100"){
    downs.io.i_data := audio.asSInt
    // }elsewhen(i2s_decoder.io.bitLength === B"010"){
    //     downs.io.i_data := audio(31 downto 0).asSInt
    // }elsewhen(i2s_decoder.io.bitLength === B"001"){
    //     downs.io.i_data := audio(31 downto 0).asSInt
    // } otherwise {
    //     downs.io.i_data := 0
    // }

    cic.io.i_div := downs.io.div
    cic.io.i_data := downs.io.o_data
    dac.io.dac_in := cic.io.o_data(43 downto 44-31).asUInt
    io.sound_dac_p := dac.io.dac_out
    io.sound_dac_n := dac.io.dac_out //!dac.io.dac_out
}

object FPGA_SDX_TOP_sim {
    def main(args: Array[String]) {
        SimConfig.withFstWave.compile{
            val dut = new FPGA_SDX_TOP()
            dut
        }.doSim { dut =>
            //Fork a process to generate the reset and the clock on the dut
            dut.clockDomain.forkStimulus(period = 10)
            dut.io.bclk #= false
            dut.io.lrclk #= true
            dut.io.sdata #= false
            var c = 0;
            var cc = 0;
            var s = 0.0;
            var t = 0.0;
            var bit = 16;
            val loop = new Breaks;
            loop.breakable {
                while (true) {
                    dut.clockDomain.waitRisingEdge()
                    if(c % 2 == 0){
                        dut.io.bclk #= !dut.io.bclk.toBoolean
                        if(dut.io.bclk.toBoolean){
                            if(cc % bit == 0){
                                dut.io.lrclk #= !dut.io.lrclk.toBoolean
                                t = c.toFloat / 3000000.0   
                                s = Math.sin(2 * Math.PI * 500.0 * t) * ((1<<(bit-1))-1)
                                //s = 0x12345678
                            }
                            dut.io.sdata #= (s.toInt >> (bit - (cc%bit)) & 0x1).toBoolean
                            cc+=1
                        }
                    }



                    c += 1
                    if(c > 99999){
                        loop.break;
                    }
                }
            }
        }
    }
}