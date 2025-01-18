MILL := "./mill"
ELABORATOR := "./out/elaborator/assembly.dest/out.jar"
BUILDFLAGS := "design --parameter ./configs/FangShan.json --target-dir ."
ANNOFILE := "FangShan.anno.json"
FIRTOOL := "firtool"
ARC := "arcilator"
LLC := "llc"
OUTDIR := "emitOut"

build:
    @echo "Building..."
    {{ MILL }} -i __.assembly
    {{ ELABORATOR }} {{ BUILDFLAGS }}

emit: build
  mkdir -p {{ OUTDIR }}
  {{ FIRTOOL }} FangShan.fir --split-verilog --annotation-file={{ ANNOFILE }} -o {{ OUTDIR }}
  {{ FIRTOOL }} FangShan.fir --ir-hw --annotation-file={{ ANNOFILE }} -o {{ OUTDIR }}/FangShan.mlir

arcilator: emit
  {{ ARC }} FangShan.mlir | {{ LLC }} -O3 -o FangShan.s

reformat:
    @echo "Reformatting..."
    {{ MILL }} -i fangshan.reformat

clean:
    @echo "Cleaning..."
    rm -rf out
    rm -rf {{ OUTDIR }}
    rm -f FangShan.mlir
    rm -f FangShan.s
    rm -f FangShan.fir
    rm -f FangShan.v