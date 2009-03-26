# =============================================================================
#
#    Remuco - A remote control system for media players.
#    Copyright (C) 2006-2009 Oben Sonne <obensonne@googlemail.com>
#
#    This file is part of Remuco.
#
#    Remuco is free software: you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation, either version 3 of the License, or
#    (at your option) any later version.
#
#    Remuco is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with Remuco.  If not, see <http://www.gnu.org/licenses/>.
#
# =============================================================================

# --- 'is known' features ---

FT_KNOWN_VOLUME = 1 << 0
FT_KNOWN_REPEAT = 1 << 1
FT_KNOWN_SHUFFLE = 1 << 2
FT_KNOWN_PLAYBACK = 1 << 3
FT_KNOWN_PROGRESS = 1 << 4

# --- control features ---

FT_CTRL_PLAYBACK = 1 << 9
FT_CTRL_VOLUME = 1 << 10
FT_CTRL_SEEK = 1 << 11
FT_CTRL_TAG = 1 << 12
#FT_CTRL_ = 1 << 13
#FT_CTRL_ = 1 << 14
FT_CTRL_RATE = 1 << 15
FT_CTRL_REPEAT = 1 << 16
FT_CTRL_SHUFFLE = 1 << 17
FT_CTRL_NEXT = 1 << 18
FT_CTRL_PREV = 1 << 19
FT_CTRL_FULLSCREEN = 1 << 20
        
# --- request features ---

FT_REQ_ITEM = 1 << 25
FT_REQ_PL = 1 << 26
FT_REQ_QU = 1 << 27
FT_REQ_MLIB = 1 << 28

# --- misc features

FT_SHUTDOWN = 1 << 30

