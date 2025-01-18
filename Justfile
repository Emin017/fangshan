MILL := "./mill"
ELABORATOR := "./out/elaborator/assembly.dest/out.jar"
BUILDFLAGS := "design --parameter ./configs/FangShan.json --target-dir ."
FIRTOOL := "firtool"
ARC := "arcilator"
LLC := "llc"

build:
    @echo "Building..."
    {{ MILL }} -i __.assembly
    {{ ELABORATOR }} {{ BUILDFLAGS }}

emit: build
  {{ FIRTOOL }} FangShan.fir -o FangShan.sv
  {{ FIRTOOL }} FangShan.fir --ir-hw -o FangShan.mlir

arcilator: emit
  {{ ARC } FangShan.mlir | {{ LLC }} -O3 -o FangShan.s
  {{ ARC }} FangShan.mlir | {{ LLC }} -O3 -o FangShan.s

reformat:
    @echo "Reformatting..."
    {{ MILL }} -i fangshan.reformat