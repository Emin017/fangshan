use std::ffi::{c_char, CString};
use std::sync::Mutex;

use crate::drive::Driver;
use crate::plusarg::PlusArgMatcher;
use crate::FangShanArgs;
use svdpi::sys::dpi::{svBit, svBitVecVal};
use svdpi::SvScope;

pub type SvBitVecVal = u32;

// --------------------------
// preparing data structures
// --------------------------

static DPI_TARGET: Mutex<Option<Box<Driver>>> = Mutex::new(None);

//----------------------
// dpi functions
//----------------------

#[no_mangle]
unsafe extern "C" fn fangshan_init() {
    let plusargs = PlusArgMatcher::from_args();
    let args = FangShanArgs::from_plusargs(&plusargs);
    args.setup_logger().unwrap();
    let scope = SvScope::get_current().expect("failed to get scope in fangshan_init");
    let driver = Box::new(Driver::new(scope, &args));

    let mut dpi_target = DPI_TARGET.lock().unwrap();
    assert!(
        dpi_target.is_none(),
        "fangshan_init should be called only once"
    );
    *dpi_target = Some(driver);

    if let Some(driver) = dpi_target.as_mut() {
        driver.init();
    }
}

#[no_mangle]
unsafe extern "C" fn fangshan_watchdog(reason: *mut c_char) {
    let mut _driver = DPI_TARGET.lock().unwrap();
    if let Some(_driver) = _driver.as_mut() {
        *reason = _driver.watchdog() as c_char;
    }
}

#[no_mangle]
unsafe extern "C" fn fangshan_input(payload: *mut svBitVecVal) {
    let mut _driver = DPI_TARGET.lock().unwrap();
    if let Some(_driver) = _driver.as_mut() {
        // TODO: implement fangshan_input
    }
}

#[no_mangle]
unsafe extern "C" fn mem_read(addr: u32, rvalid: svBit, data: *mut svBitVecVal) {
    let mut driver = DPI_TARGET.lock().unwrap();
    if let Some(driver) = driver.as_mut() {
        if rvalid != 0 && addr != 0 {
            *data = driver.read_memory(addr as usize);
        }
    }
}

//--------------------------------
// import functions and wrappers
//--------------------------------

mod dpi_export {
    use std::ffi::c_char;
    extern "C" {
        #[cfg(feature = "trace")]
        /// `export "DPI-C" function dump_wave(input string file)`
        pub fn dump_wave(path: *const c_char);
    }
}

#[cfg(feature = "trace")]
pub(crate) fn dump_wave(scope: SvScope, path: &str) {
    use svdpi::set_scope;

    let path_cstring = CString::new(path).unwrap();

    set_scope(scope);
    unsafe {
        dpi_export::dump_wave(path_cstring.as_ptr());
    }
}
