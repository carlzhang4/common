`timescale 1ns / 1ns
module tb_OffsetGenerator(

    );

reg                 clock                         =0;
reg                 reset                         =0;
reg       [7:0]     io_num                        =0;
reg       [31:0]    io_range                      =0;
reg       [31:0]    io_step                       =0;
reg                 io_en                         =0;
wire      [31:0]    io_offset                     ;


OffsetGenerator OffsetGenerator_inst(
        .*
);

/*
*/

initial begin
        reset <= 1;
        clock = 1;
        #1000;
        reset <= 0;
        #100;
		io_num		<= 8;
		io_range	<= 32'h1000;
		io_step		<= 32;
		io_en		<= 0;
		#50;
		io_en		<= 1; 
end

always #5 clock=~clock;
endmodule