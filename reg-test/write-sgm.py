#!/usr/bin/env python

# Stolen from METEOR's mt-diff.py tool
# (under the LGPL license)

import math, os, re, shutil, sys, tempfile

def main(argv):

    # Usage
    if len(argv[1:]) < 3:
        print 'usage: {0} <lang> <hyps> <out_dir> <ref1> [ref2 ...]'. \
          format(argv[0])
        print 'langs: {0}'.format(langs)
        sys.exit(1)

    # Language
    lang = argv[1]

    # Files
    hyp_file = argv[2]
    work_dir = argv[3]
    ref_files = argv[4:]

    # Work directory
    if not os.path.exists(work_dir):
        os.makedirs(work_dir)

    # SGML Files
    hyp_sgm = os.path.join(work_dir, 'hyps')
    src_sgm = os.path.join(work_dir, 'src')
    ref_sgm = os.path.join(work_dir, 'ref')

    # Hyp1
    write_sgm(hyp_file, hyp_sgm, \
      '<tstset trglang="any" setid="any" srclang="any">', '</tstset>')

    # Src (ref1)
    ref_len = write_sgm(ref_files[0], src_sgm, \
      '<srcset trglang="any" setid="any" srclang="any">', '</srcset>')

    # Ref (all refs)
    write_ref_sgm(ref_files, ref_sgm, \
      '<refset trglang="any" setid="any" srclang="any">', '</refset>')

def write_sgm(in_file, out_sgm, header, footer):
    file_in = open(in_file)
    file_out = open(out_sgm, 'w')
    print >> file_out, header
    print >> file_out, '<doc sysid="any" docid="any">'
    i = 0
    for line in file_in:
        i += 1
        print >> file_out, '<seg id="{0}"> {1} </seg>'.format(i, line.strip())
    print >> file_out, '</doc>'
    print >> file_out, footer
    file_in.close()
    file_out.close()
    return i

def write_ref_sgm(in_files, out_sgm, header, footer):
    file_out = open(out_sgm, 'w')
    print >> file_out, header
    sys_id = 0
    for in_file in in_files:
        sys_id += 1
        file_in = open(in_file)
        print >> file_out, '<doc sysid="{0}" docid="any">'.format(sys_id)
        i = 0
        for line in file_in:
            i += 1
            print >> file_out, '<seg id="{0}"> {1} </seg>'. \
              format(i, line.strip())
        print >> file_out, '</doc>'
        file_in.close()
    print >> file_out, footer
    file_out.close()

if __name__ == '__main__' : main(sys.argv)
