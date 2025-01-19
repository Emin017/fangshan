MILL := "./mill"
JAVA := "java"
ELABORATOR := "java -cp ./out/elaborator/assembly.dest/out.jar"

FIRTOOL := "firtool"
ARC := "arcilator"
LLC := "llc"

OUTDIR := "fangshan-sim-result"

BUILDNAME := "FangShan"

EMUNAME := "FangShanTestBench"

DPIDIR := "fangshanemu"

export DESIGN_DATA_WIDTH := "32"
export DESIGN_TIMEOUT := "1000"
export DESIGN_TEST_SIZE := "100"
export CLOCK_FLIP_TIME := "1"

build:
  @echo "Building..."
  {{ MILL }} -i __.assembly

dpi-lib:
    @echo "Building DPI lib..."
    @mkdir -p {{ OUTDIR }}/emu
    cd {{ DPIDIR }} && cargo build --features sv2023
    @cp {{ DPIDIR }}/target/debug/libfangshanemu.a {{ OUTDIR }}/emu

emit NAME BUILDCLASS BUILDFLAGS ANNOFILE: build
  @echo "Emitting..."
  @mkdir -p {{ OUTDIR }}
  {{ ELABORATOR }} {{ BUILDCLASS }} {{ BUILDFLAGS }}
  {{ FIRTOOL }} {{ NAME }}.fir --split-verilog --annotation-file={{ ANNOFILE }} -o {{ OUTDIR }}/emit
  {{ FIRTOOL }} {{ NAME }}.fir --ir-hw --annotation-file={{ ANNOFILE }} -o {{ OUTDIR }}/emit/{{ NAME }}.mlir

verilog: (emit "FangShan" "fangshan.elaborator.FangShanMain" "design --parameter ./configs/FangShan.json --target-dir ." "FangShan.anno.json")

emu: dpi-lib (emit "FangShanTestBench" "fangshan.elaborator.FangShanTestBenchMain" "design --parameter ./configs/FangShanTestBench.json --target-dir ." "FangShanTestBench.anno.json")
  @mkdir -p {{ OUTDIR }}/emu
  verilator --trace-fst --timing --threads 1 -Mdir {{ OUTDIR }}/emu \
    -O1 --main --exe --cc -I{{ OUTDIR }}/emit -f {{ OUTDIR }}/emit/filelist.f --top FangShanTestBench libfangshanemu.a

  echo "Building verilated C lib"
  cd {{ OUTDIR }}/emu && make -j`nproc` -f V{{ EMUNAME }}.mk V{{ EMUNAME }}

arcilator BUILDNAME:
  {{ ARC }} {{ BUILDNAME }}.mlir | {{ LLC }} -O3 -o FangShan.s

reformat:
    @echo "Reformatting..."
    {{ MILL }} -i fangshan.reformat

clean:
    @echo "Cleaning..."
    rm -rf out {{ OUTDIR }} *.fir *.mlir *.anno.json *.s