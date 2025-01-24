use tracing::{error, info, trace};

use crate::FangShanArgs;
use svdpi::{get_time, SvScope};

const BUILTIN_IMG: &[u32] = &[
    0x00100093, // addi x1, x0, 1
    0x00200113, // addi x2, x0, 2
    0x002081b3, // add x3, x1, x2
    0x00100073, // ebreak
];

pub(crate) struct Driver {
    scope: SvScope,
    pub(crate) data_width: u64,
    pub(crate) timeout: u64,
    pub(crate) test_size: u64,
    pub(crate) clock_flip_time: u64,

    #[cfg(feature = "trace")]
    wave_path: String,
    #[cfg(feature = "trace")]
    dump_start: u64,
    #[cfg(feature = "trace")]
    dump_end: u64,
    #[cfg(feature = "trace")]
    dump_started: bool,

    test_num: u64,
    last_input_cycle: u64,
    memory: Vec<u32>,
}

impl Driver {
    fn get_tick(&self) -> u64 {
        get_time() / self.clock_flip_time
    }

    pub(crate) fn new(scope: SvScope, args: &FangShanArgs) -> Self {
        let mut memory = vec![0; 1024];
        let _ = match &args.bin_path {
            Some(path) => Self::load_memory(path),
            None => {
                memory[..BUILTIN_IMG.len()].copy_from_slice(BUILTIN_IMG);
                memory
            }
        };
        Self {
            scope,
            #[cfg(feature = "trace")]
            wave_path: args.wave_path.to_owned(),
            #[cfg(feature = "trace")]
            dump_start: args.dump_start,
            #[cfg(feature = "trace")]
            dump_end: args.dump_end,
            #[cfg(feature = "trace")]
            dump_started: false,
            data_width: env!("DESIGN_DATA_WIDTH").parse().unwrap(),
            timeout: env!("DESIGN_TIMEOUT").parse().unwrap(),
            test_size: env!("DESIGN_TEST_SIZE").parse().unwrap(),
            clock_flip_time: env!("CLOCK_FLIP_TIME").parse().unwrap(),
            test_num: 0,
            last_input_cycle: 0,
            memory: vec![0; 1024],
        }
    }

    pub(crate) fn init(&mut self) {
        #[cfg(feature = "trace")]
        if self.dump_start == 0 {
            self.start_dump_wave();
            self.dump_started = true;
        }
    }

    pub(crate) fn watchdog(&mut self) -> u8 {
        const WATCHDOG_CONTINUE: u8 = 0;
        const WATCHDOG_TIMEOUT: u8 = 1;
        const WATCHDOG_FINISH: u8 = 2;

        let tick = self.get_tick();
        if self.test_num >= self.test_size {
            info!("[{tick}] test finished, exiting");
            WATCHDOG_FINISH
        } else if tick - self.last_input_cycle > self.timeout {
            error!(
                "[{}] watchdog timeout, last input tick = {}, {} tests completed",
                tick, self.last_input_cycle, self.test_num
            );
            WATCHDOG_TIMEOUT
        } else {
            #[cfg(feature = "trace")]
            if self.dump_end != 0 && tick > self.dump_end {
                info!("[{tick}] run to dump end, exiting");
                return WATCHDOG_FINISH;
            }

            #[cfg(feature = "trace")]
            if !self.dump_started && tick >= self.dump_start {
                self.start_dump_wave();
                self.dump_started = true;
            }
            trace!("[{tick}] watchdog continue");
            WATCHDOG_CONTINUE
        }
    }

    #[cfg(feature = "trace")]
    fn start_dump_wave(&mut self) {
        use crate::dpi::dump_wave;
        dump_wave(self.scope, &self.wave_path);
    }

    pub(crate) fn read_memory(&mut self, addr: u32) -> u32 {
        let index = (addr / 4) as usize;
        if index < self.memory.len() {
            self.memory[index]
        } else {
            0
        }
    }

    fn load_memory(path: &str) -> Vec<u32> {
        use std::fs::File;
        use std::io::Read;

        let mut file = File::open(path).expect("Fail to open file");
        let mut buffer = Vec::new();
        file.read_to_end(&mut buffer).expect("Read file failed");

        let mut memory = Vec::new();
        for chunk in buffer.chunks(4) {
            if chunk.len() == 4 {
                let value = u32::from_le_bytes([chunk[0], chunk[1], chunk[2], chunk[3]]);
                memory.push(value);
            }
        }
        memory
    }
}
