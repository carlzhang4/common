`timescale 1ns / 1ps
module tb_TestCompositeRouter(

    );

reg                 clock                         =0;
reg                 reset                         =0;
wire                io_in_meta_ready              ;
reg                 io_in_meta_valid              =0;
reg       [7:0]     io_in_meta_bits               =0;
wire                io_in_data_ready              ;
reg                 io_in_data_valid              =0;
reg                 io_in_data_bits_last          =0;
reg       [31:0]    io_in_data_bits_data          =0;
reg       [3:0]     io_in_data_bits_keep          =0;
reg                 io_out_meta0_ready            =0;
wire                io_out_meta0_valid            ;
wire      [7:0]     io_out_meta0_bits             ;
reg                 io_out_meta1_ready            =0;
wire                io_out_meta1_valid            ;
wire      [7:0]     io_out_meta1_bits             ;
reg                 io_out_meta2_ready            =0;
wire                io_out_meta2_valid            ;
wire      [7:0]     io_out_meta2_bits             ;
reg                 io_out_meta3_ready            =0;
wire                io_out_meta3_valid            ;
wire      [7:0]     io_out_meta3_bits             ;
reg                 io_out_data0_ready            =0;
wire                io_out_data0_valid            ;
wire                io_out_data0_bits_last        ;
wire      [31:0]    io_out_data0_bits_data        ;
wire      [3:0]     io_out_data0_bits_keep        ;
reg                 io_out_data1_ready            =0;
wire                io_out_data1_valid            ;
wire                io_out_data1_bits_last        ;
wire      [31:0]    io_out_data1_bits_data        ;
wire      [3:0]     io_out_data1_bits_keep        ;
reg                 io_out_data2_ready            =0;
wire                io_out_data2_valid            ;
wire                io_out_data2_bits_last        ;
wire      [31:0]    io_out_data2_bits_data        ;
wire      [3:0]     io_out_data2_bits_keep        ;
reg                 io_out_data3_ready            =0;
wire                io_out_data3_valid            ;
wire                io_out_data3_bits_last        ;
wire      [31:0]    io_out_data3_bits_data        ;
wire      [3:0]     io_out_data3_bits_keep        ;

IN#(8)in_io_in_meta(
        clock,
        reset,
        {io_in_meta_bits},
        io_in_meta_valid,
        io_in_meta_ready
);
// 
// 8'h0

IN#(37)in_io_in_data(
        clock,
        reset,
        {io_in_data_bits_last,io_in_data_bits_data,io_in_data_bits_keep},
        io_in_data_valid,
        io_in_data_ready
);
// last, data, keep
// 1'h0, 32'h0, 4'h0

OUT#(8)out_io_out_meta0(
        clock,
        reset,
        {io_out_meta0_bits},
        io_out_meta0_valid,
        io_out_meta0_ready
);
// 
// 8'h0

OUT#(8)out_io_out_meta1(
        clock,
        reset,
        {io_out_meta1_bits},
        io_out_meta1_valid,
        io_out_meta1_ready
);
// 
// 8'h0

OUT#(8)out_io_out_meta2(
        clock,
        reset,
        {io_out_meta2_bits},
        io_out_meta2_valid,
        io_out_meta2_ready
);
// 
// 8'h0

OUT#(8)out_io_out_meta3(
        clock,
        reset,
        {io_out_meta3_bits},
        io_out_meta3_valid,
        io_out_meta3_ready
);
// 
// 8'h0

OUT#(37)out_io_out_data0(
        clock,
        reset,
        {io_out_data0_bits_last,io_out_data0_bits_data,io_out_data0_bits_keep},
        io_out_data0_valid,
        io_out_data0_ready
);
// last, data, keep
// 1'h0, 32'h0, 4'h0

OUT#(37)out_io_out_data1(
        clock,
        reset,
        {io_out_data1_bits_last,io_out_data1_bits_data,io_out_data1_bits_keep},
        io_out_data1_valid,
        io_out_data1_ready
);
// last, data, keep
// 1'h0, 32'h0, 4'h0

OUT#(37)out_io_out_data2(
        clock,
        reset,
        {io_out_data2_bits_last,io_out_data2_bits_data,io_out_data2_bits_keep},
        io_out_data2_valid,
        io_out_data2_ready
);
// last, data, keep
// 1'h0, 32'h0, 4'h0

OUT#(37)out_io_out_data3(
        clock,
        reset,
        {io_out_data3_bits_last,io_out_data3_bits_data,io_out_data3_bits_keep},
        io_out_data3_valid,
        io_out_data3_ready
);
// last, data, keep
// 1'h0, 32'h0, 4'h0


TestCompositeRouter TestCompositeRouter_inst(
        .*
);


initial begin
        reset <= 1;
        clock = 1;
        #1000;
        reset <= 0;
        #100;
        out_io_out_meta0.start();
        out_io_out_meta1.start();
        out_io_out_meta2.start();
        out_io_out_meta3.start();
		out_io_out_data0.start();
        out_io_out_data1.start();
        out_io_out_data2.start();
        out_io_out_data3.start();
        #50;
		//test1: basic
        in_io_in_meta.write({8'h0});
        in_io_in_meta.write({8'h1});
        in_io_in_meta.write({8'h2});
        in_io_in_meta.write({8'h3});
		in_io_in_meta.write({8'h0});
        in_io_in_meta.write({8'h1});
        in_io_in_meta.write({8'h2});
        in_io_in_meta.write({8'h3});
        in_io_in_data.write({1'h1, 32'h0, 4'h0});
        in_io_in_data.write({1'h1, 32'h1, 4'h0});
        in_io_in_data.write({1'h1, 32'h2, 4'h0});
        in_io_in_data.write({1'h1, 32'h3, 4'h0});
		in_io_in_data.write({1'h1, 32'h0, 4'h0});
        in_io_in_data.write({1'h1, 32'h1, 4'h0});
        in_io_in_data.write({1'h1, 32'h2, 4'h0});
        in_io_in_data.write({1'h1, 32'h3, 4'h0});

		#1000;
		//test2: more than 1 beat
		in_io_in_meta.write({8'h0});
        in_io_in_meta.write({8'h1});
        in_io_in_meta.write({8'h2});
        in_io_in_meta.write({8'h3});
		in_io_in_meta.write({8'h0});
        in_io_in_meta.write({8'h1});
        in_io_in_meta.write({8'h2});
        in_io_in_meta.write({8'h3});
        in_io_in_data.write({1'h0, 32'h0, 4'h0});
        in_io_in_data.write({1'h0, 32'h0, 4'h0});
        in_io_in_data.write({1'h1, 32'h0, 4'h0});
        in_io_in_data.write({1'h0, 32'h1, 4'h0});
        in_io_in_data.write({1'h0, 32'h1, 4'h0});
        in_io_in_data.write({1'h1, 32'h1, 4'h0});
        in_io_in_data.write({1'h0, 32'h2, 4'h0});
        in_io_in_data.write({1'h0, 32'h2, 4'h0});
        in_io_in_data.write({1'h1, 32'h2, 4'h0});
        in_io_in_data.write({1'h0, 32'h3, 4'h0});
        in_io_in_data.write({1'h0, 32'h3, 4'h0});
        in_io_in_data.write({1'h1, 32'h3, 4'h0});
		in_io_in_data.write({1'h0, 32'h0, 4'h0});
		in_io_in_data.write({1'h0, 32'h0, 4'h0});
        in_io_in_data.write({1'h1, 32'h0, 4'h0});
        in_io_in_data.write({1'h0, 32'h1, 4'h0});
        in_io_in_data.write({1'h0, 32'h1, 4'h0});
        in_io_in_data.write({1'h1, 32'h1, 4'h0});
        in_io_in_data.write({1'h0, 32'h2, 4'h0});
        in_io_in_data.write({1'h0, 32'h2, 4'h0});
        in_io_in_data.write({1'h1, 32'h2, 4'h0});
        in_io_in_data.write({1'h0, 32'h3, 4'h0});
        in_io_in_data.write({1'h0, 32'h3, 4'h0});
        in_io_in_data.write({1'h1, 32'h3, 4'h0});

		#1000;
		//test3: cmd come, data later
		in_io_in_meta.write({8'h0});
        in_io_in_meta.write({8'h1});
        in_io_in_meta.write({8'h2});
        in_io_in_meta.write({8'h3});
        #100;
		in_io_in_data.write({1'h0, 32'h0, 4'h0});
		in_io_in_data.write({1'h0, 32'h0, 4'h0});
        in_io_in_data.write({1'h1, 32'h0, 4'h0});
        in_io_in_data.write({1'h0, 32'h1, 4'h0});
        in_io_in_data.write({1'h0, 32'h1, 4'h0});
        in_io_in_data.write({1'h1, 32'h1, 4'h0});
        in_io_in_data.write({1'h0, 32'h2, 4'h0});
        in_io_in_data.write({1'h0, 32'h2, 4'h0});
        in_io_in_data.write({1'h1, 32'h2, 4'h0});
        in_io_in_data.write({1'h0, 32'h3, 4'h0});
        in_io_in_data.write({1'h0, 32'h3, 4'h0});
        in_io_in_data.write({1'h1, 32'h3, 4'h0});

		#1000;
		//test4: data come, cmd later
		in_io_in_data.write({1'h0, 32'h0, 4'h0});
		in_io_in_data.write({1'h0, 32'h0, 4'h0});
        in_io_in_data.write({1'h1, 32'h0, 4'h0});
        in_io_in_data.write({1'h0, 32'h1, 4'h0});
        in_io_in_data.write({1'h0, 32'h1, 4'h0});
        in_io_in_data.write({1'h1, 32'h1, 4'h0});
        in_io_in_data.write({1'h0, 32'h2, 4'h0});
        in_io_in_data.write({1'h0, 32'h2, 4'h0});
        in_io_in_data.write({1'h1, 32'h2, 4'h0});
        in_io_in_data.write({1'h0, 32'h3, 4'h0});
        in_io_in_data.write({1'h0, 32'h3, 4'h0});
        in_io_in_data.write({1'h1, 32'h3, 4'h0});
		#100;
		in_io_in_meta.write({8'h0});
        in_io_in_meta.write({8'h1});
        in_io_in_meta.write({8'h2});
        in_io_in_meta.write({8'h3});
end
always #5 clock=~clock;

endmodule