/*
 *   Copyright 2010 MINT Working Group
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.nema.medical.mint.dcm2mint;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Uli Bubenheimer
 */
public final class BinaryFileData implements BinaryData {
    /** The study's binary data */
    public final List<File> binaryItems = new ArrayList<File>();

    @Override
    public void add(final byte[] item) {
        //Stream to file
        assert item != null;
        try {
            //The file should be deleted as soon as it is passed on to somewhere else;
            //using SelfDeletingFile is just a backup,
            final File tmpFile = new SelfDeletingFile(File.createTempFile("mint", ".bin"));
            final FileOutputStream outStream = new FileOutputStream(tmpFile);
            try {
                outStream.write(item);
            } finally {
                outStream.close();
            }

            binaryItems.add(tmpFile);
        } catch (final IOException ex) {
         //This would happen only in a catastrophic case
         throw new RuntimeException(ex);
        }
    }

    public File getBinaryFile(final int index) {
         return binaryItems.get(index);
    }

    @Override
    public byte[] getBinaryItem(final int index) {
         final File binaryItem = getBinaryFile(index);
         return fileToByteArray(binaryItem);
    }

    @Override
    public int size() {
        return binaryItems.size();
    }

    public Iterator<File> fileIterator() {
        return binaryItems.iterator();
    }

    @Override
    public Iterator<byte[]> iterator() {
        return new Iterator<byte[]>() {

            @Override
            public boolean hasNext() {
                return fileIterator.hasNext();
            }

            @Override
            public byte[] next() {
                final File nextFile = fileIterator.next();
                return fileToByteArray(nextFile);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            private final Iterator<File> fileIterator = fileIterator();
        };
    }

    private static final byte[] fileToByteArray(final File file) {
        final long fileSize = file.length();
        //This should always be true, since we also wrote it from a byte array (which is bound by Integer.MAX_VALUE)
        assert fileSize <= Integer.MAX_VALUE;
        final byte[] dataBytes = new byte[(int)fileSize];
        try {
            final FileInputStream binStream = new FileInputStream(file);
            try {
                int offset = 0;
                for(;;) {
                    final int bytesRead = binStream.read(dataBytes, 0, (int)fileSize - offset);
                    if (bytesRead == 0) {
                        break;
                    }
                    offset += bytesRead;
                }
            } finally {
                binStream.close();
            }
        } catch (final IOException ex) {
            //This would happen only in a catastrophic case
            throw new RuntimeException(ex);
        }
        return dataBytes;
    }

    private static class SelfDeletingFile extends File {
        private static final long serialVersionUID = 1L;

        public SelfDeletingFile(final File origFile) {
            super(origFile.getParentFile(), origFile.getName());
            //Make sure to delete no later than when JVM goes away
            deleteOnExit();
        }

        @Override
        protected void finalize() throws Throwable {
            //Delete on finalization no matter what; if file does not exist, this will fail silently.
            delete();
        }
    }
}
