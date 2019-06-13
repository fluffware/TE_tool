import struct
import sys
import argparse

def read_block(file):
    head = file.read(2)
    if len(head) != 2: raise Exception("Failed to read block header")
    (blen,) = struct.unpack("<H", head)
    if blen > file.left: raise Exception("Not enough data left for block")
    content = file.read(blen-2)
    file.left -= blen
    return content

def get_strz(raw, start):
    end = raw.find(b"\0", start)
    if end == -1:
        raise Exception("String is not zero terminated")
    string = str(block[start:end], "ISO8859-1")
    end += 1
    if end < len(raw) and raw[end] == 0xff:
        end += 1
    return (string, end)

def print_block(block):
    print(("# %3d; " % (len(block)+2)) + " ".join("%02x" % b for b in block))
    
parser = argparse.ArgumentParser(description='Print content of project file.')
parser.add_argument('file', metavar='FILE', type=str,
                    help='input file')
parser.add_argument("-b", "--print-blocks", dest="dump_blocks",
                    action='store_const',
                    const=True, default=False,
                    help='print raw block contents')


args = parser.parse_args()


dump_blocks = args.dump_blocks


file = open(args.file,"rb")
file.left = 16
block = read_block(file)
(left,) = struct.unpack("<H",block[0:2])
file.left = left - (len(block) + 2)
if dump_blocks:
    print_block(block)

print("Header:");
(flen, screens, default, p1,p2, interface, p3) = struct.unpack("<HHHHHHH",block)
if interface == 1:
    print("  USB interface");
elif interface == 2:
    print("  CAN interface");
else:
    print("Unknown interface")
print(f"  {screens:d} screens")
if p1 != 1 or p2 != 1 or p3 != 0:
    raise Exception("Unknown values in header block")

while file.left > 0:
    header = read_block(file)
    if dump_blocks:
        print_block(header)

    (wno, subwno, grp1_cnt, grp2_cnt, grp3_cnt) = struct.unpack("<BBHHH", header);
    print("Screen %d.%d:" % (wno,subwno))
    print("  Variables:")
    for g in range(0, grp1_cnt):
        block = read_block(file)
        if dump_blocks:
            print_block(block)
        (index,p1,start, min, max, step,disp,flags) = struct.unpack("<BBhhhhBB", block)
        if (p1 & 0xfc) != 0 or (flags & 0xfe) != 0:
            raise Exception("Unknown values in variable block")
        print("    Variable %d: %d <= v <= %d, start %d, step %d, %s %s, disp %02x, flags: %02x"
              % (index, min,max, start, step,
                 "Encoder" if (p1 & 1) != 0 else "",
                 "Host" if (p1 & 2) != 0 else "", disp,flags))
        
    print("  Visual elements:")
    for g in range(0, grp2_cnt):
        block = read_block(file)
        if dump_blocks:
            print_block(block)
        (index, type) = struct.unpack("<HH", block[0:4])
        if type == 1 or type == 0:
            (f2,f3) = struct.unpack("<HH", block[4:8])
            if f2 != 0 or f3 != 0:
                raise Exception("Unknown values in background image block")
            print("    Background image: \"%s\"" % (get_strz(block,8)[0]))
        elif type == 5:
            (empty, next) = get_strz(block, 16)
            (full, next) = get_strz(block, next)
            (cursor, next) = get_strz(block, next)
            print("    Empty scale: \"%s\"" % empty)
            print("    Full scale: \"%s\"" % full)
            print("    Cursor: \"%s\"" % cursor)
        elif type == 4:
            (x,y,b,g,r,p2,font,fsize, flags, value) = struct.unpack("<HHBBBBBBBB", block[4:16])
            if p2 != 0xff:
                raise Exception("Unexpected value in text block")
            next = 16
            print("    Text: Pos (%d, %d) , Color #%02x%02x%02x, Fontsize %d, FontIndex %d, Value %d" % (x,y, r,g,b,fsize,font, value))
            if flags & 0x01:
                (prefix, next) = get_strz(block, next)
                print("      Prefix: \"%s\"" % prefix)
            if flags & 0x04:
                (suffix, next) = get_strz(block, next)
                print("      Suffix: \"%s\"" % suffix)
    print("  Events:")
    for g in range(0, grp3_cnt):
        block = read_block(file)
        if dump_blocks:
            print_block(block)
        (type,) = struct.unpack("<H", block[0:2])
        if type == 1:
            (x,y,w,h,func, index, value) = struct.unpack("<HHHHBBH", block[2:])
            print("    Press %d,%d %dx%d" % (x,y,w,h), end=": ")
            if func == 2:
                print("Set value %d to %d" % (index, value))
            elif func == 1:
                print("Goto #%d" % value)
            else:
                print("Unknown operation")
        elif type == 2:
            (u,d,l,r) = struct.unpack("<HHHH", block[2:])
            print("    Swipe: Up #%d, Down #%d, Left #%d, Right #%d" % (u,d,l,r))
        elif type == 3:
            (cw,ccw) = struct.unpack("<HH", block[2:])
            print("    Rotate: CW #%d, CCW #%d" % (cw,ccw))
        else:
            print("    Unknown event")
