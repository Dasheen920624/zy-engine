#!/usr/bin/env perl
# 中文 CJK 字符检测工具，被 scripts/check-comment-zh.sh 调用。
# 跨平台兼容（GNU grep -P 在 macOS BSD grep 上不可用，所以独立成 perl 脚本）。
#
# 用法：
#   perl cjk-detect.pl stream                  # 从 stdin 读，含 CJK 退出 0，否则 1
#   perl cjk-detect.pl sql-table-comment FILE TABLE   # 文件中是否含 COMMENT ON TABLE <TABLE> IS '...CJK...'
use strict;
use warnings;
use utf8;

my $mode = shift @ARGV // 'stream';

if ($mode eq 'stream') {
    binmode STDIN, ':utf8';
    while (my $line = <STDIN>) {
        if ($line =~ /[\x{4e00}-\x{9fa5}]/) { exit 0; }
    }
    exit 1;
} elsif ($mode eq 'sql-table-comment') {
    my ($file, $table) = @ARGV;
    defined $file and defined $table or die "用法：cjk-detect.pl sql-table-comment FILE TABLE";
    open(my $fh, '<:utf8', $file) or die "$file: $!";
    my $re = qr/COMMENT\s+ON\s+TABLE\s+\Q$table\E\s+IS\s+'[^']*[\x{4e00}-\x{9fa5}]/i;
    while (my $line = <$fh>) {
        if ($line =~ $re) { exit 0; }
    }
    exit 1;
} else {
    die "未知模式：$mode";
}
