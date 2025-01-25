MILL := "./mill"
JAVA := "java"
ELABORATOR := "java -cp ./out/elaborator/assembly.dest/out.jar"

FIRTOOL := "firtool"
ARC := "arcilator"
LLC := "llc"
OUTDIR := "emitOut"

BUILDNAME := "FangShan"

EMUNAME := "FangShanTestBench"
EMUDIR := "fangshan-sim-result"

DPIDIR := "fangshanemu"

build:
  @echo "Building..."
  {{ MILL }} -i __.assembly

dpi-lib:
    @echo "Building DPI lib..."
    cd {{ DPIDIR }} && cargo build

emit NAME BUILDCLASS BUILDFLAGS ANNOFILE: build
  @echo "Emitting..."
  mkdir -p {{ OUTDIR }}
  {{ ELABORATOR }} {{ BUILDCLASS }} {{ BUILDFLAGS }}
  {{ FIRTOOL }} {{ NAME }}.fir --split-verilog --annotation-file={{ ANNOFILE }} -o {{ OUTDIR }}
  {{ FIRTOOL }} {{ NAME }}.fir --ir-hw --annotation-file={{ ANNOFILE }} -o {{ OUTDIR }}/{{ NAME }}.mlir

verilog: (emit "FangShan" "fangshan.elaborator.FangShanMain" "design --parameter ./configs/FangShan.json --target-dir ." "FangShan.anno.json")

emu: (emit "FangShanTestBench" "fangshan.elaborator.FangShanTestBenchMain" "design --parameter ./configs/FangShanTestBench.json --target-dir ." "FangShanTestBench.anno.json")
  verilator --trace-fst --timing --threads 1 -Mdir {{ EMUDIR }} \
    -O1 --main --exe --cc -I{{ OUTDIR }} -f {{ OUTDIR }}/filelist.f --top FangShanTestBench

  echo "Building verilated C lib"
  cd {{ EMUDIR }} && make -j`nproc` -f V{{ EMUNAME }}.mk V{{ EMUNAME }}

arcilator BUILDNAME:
  {{ ARC }} {{ BUILDNAME }}.mlir | {{ LLC }} -O3 -o FangShan.s

reformat:
    @echo "Reformatting..."
    {{ MILL }} -i fangshan.reformat

clean:
    @echo "Cleaning..."
    rm -rf out {{ OUTDIR }} *.fir *.mlir *.anno.json *.s