`timescale 1ns / 1ps
module tb_TestCompositeArbiter(

    );

reg                 clock                         =0;
reg                 reset                         =0;
wire                io_in_meta0_ready             ;
reg                 io_in_meta0_valid             =0;
reg       [7:0]     io_in_meta0_bits              =0;
wire                io_in_meta1_ready             ;
reg                 io_in_meta1_valid             =0;
reg       [7:0]     io_in_meta1_bits              =0;
wire                io_in_meta2_ready             ;
reg                 io_in_meta2_valid             =0;
reg       [7:0]     io_in_meta2_bits              =0;
wire                io_in_meta3_ready             ;
reg                 io_in_meta3_valid             =0;
reg       [7:0]     io_in_meta3_bits              =0;
wire                io_in_data0_ready             ;
reg                 io_in_data0_valid             =0;
reg                 io_in_data0_bits_last         =0;
reg       [31:0]    io_in_data0_bits_data         =0;
reg       [3:0]     io_in_data0_bits_keep         =0;
wire                io_in_data1_ready             ;
reg                 io_in_data1_valid             =0;
reg                 io_in_data1_bits_last         =0;
reg       [31:0]    io_in_data1_bits_data         =0;
reg       [3:0]     io_in_data1_bits_keep         =0;
wire                io_in_data2_ready             ;
reg                 io_in_data2_valid             =0;
reg                 io_in_data2_bits_last         =0;
reg       [31:0]    io_in_data2_bits_data         =0;
reg       [3:0]     io_in_data2_bits_keep         =0;
wire                io_in_data3_ready             ;
reg                 io_in_data3_valid             =0;
reg                 io_in_data3_bits_last         =0;
reg       [31:0]    io_in_data3_bits_data         =0;
reg       [3:0]     io_in_data3_bits_keep         =0;
reg                 io_out_meta_ready             =0;
wire                io_out_meta_valid             ;
wire      [7:0]     io_out_meta_bits              ;
reg                 io_out_data_ready             =0;
wire                io_out_data_valid             ;
wire                io_out_data_bits_last         ;
wire      [31:0]    io_out_data_bits_data         ;
wire      [3:0]     io_out_data_bits_keep         ;

IN#(8)in_io_in_meta0(
        clock,
        reset,
        {io_in_meta0_bits},
        io_in_meta0_valid,
        io_in_meta0_ready
);
// 
// 8'h0

IN#(8)in_io_in_meta1(
        clock,
        reset,
        {io_in_meta1_bits},
        io_in_meta1_valid,
        io_in_meta1_ready
);
// 
// 8'h0

IN#(8)in_io_in_meta2(
        clock,
        reset,
        {io_in_meta2_bits},
        io_in_meta2_valid,
        io_in_meta2_ready
);
// 
// 8'h0

IN#(8)in_io_in_meta3(
        clock,
        reset,
        {io_in_meta3_bits},
        io_in_meta3_valid,
        io_in_meta3_ready
);
// 
// 8'h0

IN#(37)in_io_in_data0(
        clock,
        reset,
        {io_in_data0_bits_last,io_in_data0_bits_data,io_in_data0_bits_keep},
        io_in_data0_valid,
        io_in_data0_ready
);
// last, data, keep
// 1'h0, 32'h0, 4'h0

IN#(37)in_io_in_data1(
        clock,
        reset,
        {io_in_data1_bits_last,io_in_data1_bits_data,io_in_data1_bits_keep},
        io_in_data1_valid,
        io_in_data1_ready
);
// last, data, keep
// 1'h0, 32'h0, 4'h0

IN#(37)in_io_in_data2(
        clock,
        reset,
        {io_in_data2_bits_last,io_in_data2_bits_data,io_in_data2_bits_keep},
        io_in_data2_valid,
        io_in_data2_ready
);
// last, data, keep
// 1'h0, 32'h0, 4'h0

IN#(37)in_io_in_data3(
        clock,
        reset,
        {io_in_data3_bits_last,io_in_data3_bits_data,io_in_data3_bits_keep},
        io_in_data3_valid,
        io_in_data3_ready
);
// last, data, keep
// 1'h0, 32'h0, 4'h0

OUT#(8)out_io_out_meta(
        clock,
        reset,
        {io_out_meta_bits},
        io_out_meta_valid,
        io_out_meta_ready
);
// 
// 8'h0

OUT#(37)out_io_out_data(
        clock,
        reset,
        {io_out_data_bits_last,io_out_data_bits_data,io_out_data_bits_keep},
        io_out_data_valid,
        io_out_data_ready
);
// last, data, keep
// 1'h0, 32'h0, 4'h0


TestCompositeArbiter TestCompositeArbiter_inst(
        .*
);


initial begin
        reset <= 1;
        clock = 1;
        #1000;
        reset <= 0;
        #100;
        out_io_out_meta.start();
        out_io_out_data.start();
        #50;
		//basic
        in_io_in_meta0.write({8'h0});
        in_io_in_meta1.write({8'h1});
        in_io_in_meta2.write({8'h2});
        in_io_in_meta3.write({8'h3});

        in_io_in_data0.write({1'h1, 32'h0, 4'h0});
        in_io_in_data1.write({1'h1, 32'h1, 4'h0});
        in_io_in_data2.write({1'h1, 32'h2, 4'h0});
        in_io_in_data3.write({1'h1, 32'h3, 4'h0});

		#1000;
		//more than 1 beat
        in_io_in_meta0.write({8'h0});
        in_io_in_meta1.write({8'h1});
        in_io_in_meta2.write({8'h2});
        in_io_in_meta3.write({8'h3});

        in_io_in_data0.write({1'h0, 32'h0, 4'h0});
        in_io_in_data0.write({1'h0, 32'h0, 4'h0});
        in_io_in_data0.write({1'h1, 32'h0, 4'h0});
		in_io_in_data1.write({1'h0, 32'h1, 4'h0});
		in_io_in_data1.write({1'h0, 32'h1, 4'h0});
        in_io_in_data1.write({1'h1, 32'h1, 4'h0});
		in_io_in_data2.write({1'h0, 32'h2, 4'h0});
		in_io_in_data2.write({1'h0, 32'h2, 4'h0});
        in_io_in_data2.write({1'h1, 32'h2, 4'h0});
		in_io_in_data3.write({1'h0, 32'h3, 4'h0});
		in_io_in_data3.write({1'h0, 32'h3, 4'h0});
        in_io_in_data3.write({1'h1, 32'h3, 4'h0});

		#1000;
		//round robin 1 beat
		in_io_in_meta0.write({8'h0});
		in_io_in_meta0.write({8'h0});
		in_io_in_meta0.write({8'h0});
		in_io_in_meta0.write({8'h0});
        in_io_in_meta1.write({8'h1});
        in_io_in_meta1.write({8'h1});
        in_io_in_meta1.write({8'h1});
        in_io_in_meta1.write({8'h1});
        in_io_in_meta2.write({8'h2});
        in_io_in_meta2.write({8'h2});
        in_io_in_meta2.write({8'h2});
        in_io_in_meta2.write({8'h2});
        in_io_in_meta3.write({8'h3});
        in_io_in_meta3.write({8'h3});
        in_io_in_meta3.write({8'h3});
        in_io_in_meta3.write({8'h3});

        in_io_in_data0.write({1'h1, 32'h0, 4'h0});
        in_io_in_data0.write({1'h1, 32'h0, 4'h0});
        in_io_in_data0.write({1'h1, 32'h0, 4'h0});
        in_io_in_data0.write({1'h1, 32'h0, 4'h0});
        in_io_in_data1.write({1'h1, 32'h1, 4'h0});
        in_io_in_data1.write({1'h1, 32'h1, 4'h0});
        in_io_in_data1.write({1'h1, 32'h1, 4'h0});
        in_io_in_data1.write({1'h1, 32'h1, 4'h0});
        in_io_in_data2.write({1'h1, 32'h2, 4'h0});
        in_io_in_data2.write({1'h1, 32'h2, 4'h0});
        in_io_in_data2.write({1'h1, 32'h2, 4'h0});
        in_io_in_data2.write({1'h1, 32'h2, 4'h0});
        in_io_in_data3.write({1'h1, 32'h3, 4'h0});
        in_io_in_data3.write({1'h1, 32'h3, 4'h0});
        in_io_in_data3.write({1'h1, 32'h3, 4'h0});
        in_io_in_data3.write({1'h1, 32'h3, 4'h0});

		#1000;
		//round robin 3 beat
		in_io_in_meta0.write({8'h0});
		in_io_in_meta0.write({8'h0});
		in_io_in_meta0.write({8'h0});
        in_io_in_meta1.write({8'h1});
        in_io_in_meta1.write({8'h1});
        in_io_in_meta1.write({8'h1});
        in_io_in_meta2.write({8'h2});
        in_io_in_meta2.write({8'h2});
        in_io_in_meta2.write({8'h2});
        in_io_in_meta3.write({8'h3});
        in_io_in_meta3.write({8'h3});
        in_io_in_meta3.write({8'h3});

		in_io_in_data0.write({1'h0, 32'h0, 4'h0});
        in_io_in_data0.write({1'h0, 32'h0, 4'h0});
        in_io_in_data0.write({1'h1, 32'h0, 4'h0});
		in_io_in_data0.write({1'h0, 32'h0, 4'h0});
        in_io_in_data0.write({1'h0, 32'h0, 4'h0});
        in_io_in_data0.write({1'h1, 32'h0, 4'h0});
		in_io_in_data0.write({1'h0, 32'h0, 4'h0});
        in_io_in_data0.write({1'h0, 32'h0, 4'h0});
        in_io_in_data0.write({1'h1, 32'h0, 4'h0});

		in_io_in_data1.write({1'h0, 32'h1, 4'h0});
		in_io_in_data1.write({1'h0, 32'h1, 4'h0});
        in_io_in_data1.write({1'h1, 32'h1, 4'h0});
		in_io_in_data1.write({1'h0, 32'h1, 4'h0});
		in_io_in_data1.write({1'h0, 32'h1, 4'h0});
        in_io_in_data1.write({1'h1, 32'h1, 4'h0});
		in_io_in_data1.write({1'h0, 32'h1, 4'h0});
		in_io_in_data1.write({1'h0, 32'h1, 4'h0});
        in_io_in_data1.write({1'h1, 32'h1, 4'h0});

		in_io_in_data2.write({1'h0, 32'h2, 4'h0});
		in_io_in_data2.write({1'h0, 32'h2, 4'h0});
        in_io_in_data2.write({1'h1, 32'h2, 4'h0});
		in_io_in_data2.write({1'h0, 32'h2, 4'h0});
		in_io_in_data2.write({1'h0, 32'h2, 4'h0});
        in_io_in_data2.write({1'h1, 32'h2, 4'h0});
		in_io_in_data2.write({1'h0, 32'h2, 4'h0});
		in_io_in_data2.write({1'h0, 32'h2, 4'h0});
        in_io_in_data2.write({1'h1, 32'h2, 4'h0});

		in_io_in_data3.write({1'h0, 32'h3, 4'h0});
		in_io_in_data3.write({1'h0, 32'h3, 4'h0});
        in_io_in_data3.write({1'h1, 32'h3, 4'h0});
		in_io_in_data3.write({1'h0, 32'h3, 4'h0});
		in_io_in_data3.write({1'h0, 32'h3, 4'h0});
        in_io_in_data3.write({1'h1, 32'h3, 4'h0});
		in_io_in_data3.write({1'h0, 32'h3, 4'h0});
		in_io_in_data3.write({1'h0, 32'h3, 4'h0});
        in_io_in_data3.write({1'h1, 32'h3, 4'h0});


		#1000;
		//choose 1 or 3
        in_io_in_meta1.write({8'h1});
        in_io_in_meta3.write({8'h3});

		in_io_in_data1.write({1'h0, 32'h1, 4'h0});
        in_io_in_data1.write({1'h1, 32'h1, 4'h0});
		in_io_in_data3.write({1'h0, 32'h3, 4'h0});
        in_io_in_data3.write({1'h1, 32'h3, 4'h0});

		#1000;
		//cmd come, then data
        in_io_in_meta1.write({8'h1});
        in_io_in_meta3.write({8'h3});
		#100;
		in_io_in_data1.write({1'h0, 32'h1, 4'h0});
        in_io_in_data1.write({1'h1, 32'h1, 4'h0});
		in_io_in_data3.write({1'h0, 32'h3, 4'h0});
        in_io_in_data3.write({1'h1, 32'h3, 4'h0});

		#1000;
		//data come, then cmd
		in_io_in_meta3.write({8'h3});
		in_io_in_data2.write({1'h0, 32'h2, 4'h0});
		in_io_in_data2.write({1'h0, 32'h2, 4'h0});
		in_io_in_data2.write({1'h0, 32'h2, 4'h0});
        in_io_in_data2.write({1'h1, 32'h2, 4'h0});
		#100;
		in_io_in_meta2.write({8'h2});
		in_io_in_data3.write({1'h0, 32'h3, 4'h0});
        in_io_in_data3.write({1'h1, 32'h3, 4'h0});

end
always #5 clock=~clock;

endmodule