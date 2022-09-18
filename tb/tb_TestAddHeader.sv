`timescale 1ns / 1ns
module tb_TestAddHeader(

    );

reg                 clock                         =0;
reg                 reset                         =0;
wire                io_in_meta_ready              ;
reg                 io_in_meta_valid              =0;
reg       [31:0]    io_in_meta_bits               =0;
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

IN#(32)in_io_in_meta(
        clock,
        reset,
        {io_in_meta_bits},
        io_in_meta_valid,
        io_in_meta_ready
);
// 
// 32'h0

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


TestAddHeader TestAddHeader_inst(
        .*
);

/*

in_io_in_meta.write({32'h0});

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
        #50;
		in_io_in_meta.write({32'h1111});
		#50;
		in_io_in_data.write({1'h0,512'h01,64'hFFFFFFFFFFFFFFFF});
		in_io_in_data.write({1'h1,512'h02,64'hFFFFFFFFFFFFFFFF});

		#100;
		in_io_in_data.write({1'h0,512'h01,64'hFFFFFFFFFFFFFFFF});
		in_io_in_data.write({1'h1,512'h02,64'hFFFFFFFFFFFFFFFF});
		 #50;
		in_io_in_meta.write({32'h1111});

		#100;
		in_io_in_meta.write({32'h1111});
		in_io_in_data.write({1'h0,512'h01,64'hFFFFFFFFFFFFFFFF});
		in_io_in_data.write({1'h1,512'h02,64'h0FFFFFFFFFFFFFFF});


end

always #5 clock=~clock;
endmodule