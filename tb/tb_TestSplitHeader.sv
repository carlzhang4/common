`timescale 1ns / 1ns
module tb_TestSplitHeader(

    );

reg                 clock                         =0;
reg                 reset                         =0;
wire                io_in_data_ready              ;
reg                 io_in_data_valid              =0;
reg                 io_in_data_bits_last          =0;
reg       [511:0]   io_in_data_bits_data          =0;
reg       [63:0]    io_in_data_bits_keep          =0;
reg                 io_out_data_ready             =0;
wire                io_out_data_valid             ;
wire                io_out_data_bits_last         ;
wire      [511:0]   io_out_data_bits_data         ;
wire      [63:0]    io_out_data_bits_keep         ;
reg                 io_out_meta_ready             =0;
wire                io_out_meta_valid             ;
wire      [31:0]    io_out_meta_bits              ;

IN#(577)in_io_in_data(
        clock,
        reset,
        {io_in_data_bits_last,io_in_data_bits_data,io_in_data_bits_keep},
        io_in_data_valid,
        io_in_data_ready
);
// last, data, keep
// 1'h0, 512'h0, 64'h0

OUT#(577)out_io_out_data(
        clock,
        reset,
        {io_out_data_bits_last,io_out_data_bits_data,io_out_data_bits_keep},
        io_out_data_valid,
        io_out_data_ready
);
// last, data, keep
// 1'h0, 512'h0, 64'h0

OUT#(32)out_io_out_meta(
        clock,
        reset,
        {io_out_meta_bits},
        io_out_meta_valid,
        io_out_meta_ready
);
// 
// 32'h0


TestSplitHeader TestSplitHeader_inst(
        .*
);

/*
last,data,keep
in_io_in_data.write({1'h0,512'h0,64'h0});

*/

initial begin
        reset <= 1;
        clock = 1;
        #1000;
        reset <= 0;
        #100;
        out_io_out_data.start();
        out_io_out_meta.start();
        #50;
		in_io_in_data.write({1'h1,512'h01,64'hFFFFFFFFFFFFFFFF});
		in_io_in_data.write({1'h0,512'h01,64'hFFFFFFFFFFFFFFFF});
		in_io_in_data.write({1'h0,512'h02,64'hFFFFFFFFFFFFFFFF});
		in_io_in_data.write({1'h1,512'h03,64'hFFFFFFFFFFFFFFFF});
		in_io_in_data.write({1'h0,512'h03,64'hFFFFFFFFFFFFFFFF});
		in_io_in_data.write({1'h1,512'h03,64'hF});
end

always #5 clock=~clock;
endmodule