package example.Advanced.Music.app.services;

import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class ImageStorageService implements IStorageService {
    private final Path storageFolder = Paths.get("uploads/images");
    public ImageStorageService() {
        try{
            Files.createDirectories(storageFolder);
            File directory = new File("uploads/images");
            System.out.println(directory.exists()); // Kiểm tra xem thư mục có tồn tại hay không
            System.out.println(directory.isDirectory()); // Kiểm tra xem đó có phải là thư mục hay không
            System.out.println(directory.canWrite()); // Kiểm tra quyền ghi
        } catch (IOException exception) {
            throw new RuntimeException("Cannot initialize storage", exception);
        }
    }

    private boolean isImageFile(MultipartFile file) {
        //let install FileNameUtils
        String fileExtension = FilenameUtils.getExtension(file.getOriginalFilename()); // dùng hàm getExtension để lấy đuôi file
        return Arrays.asList(new String[] {"png","jpg","jpeg","bmp"})
                .contains(fileExtension.trim().toLowerCase());
    }
    @Override
    public String storeFile(MultipartFile file) {
        try {
            System.out.println("haha");
            if (file.isEmpty()){
                throw new RuntimeException("Failed to store empty file.");
            }
            //check file is image?
            if (!isImageFile(file)) {
                throw new RuntimeException("You can only upload image file.");
            }
            //file must be <= 5Mb
            float fileSizeInMegabytes = file.getSize() / 1_000_000.0f;
            if (fileSizeInMegabytes > 5.0f){
                throw new RuntimeException("File must be <= 5MB");
            }

            String generatedFilename = file.getOriginalFilename();
            String fileExtension = FilenameUtils.getExtension(file.getOriginalFilename());
            String generatedFileName2 = UUID.randomUUID().toString().replace("-", "");
            generatedFileName2 = generatedFilename + generatedFileName2+"."+ fileExtension;
            Path destinationFilePath = this.storageFolder.resolve(Paths.get(generatedFileName2))
                    .normalize().toAbsolutePath();
            if (!destinationFilePath.getParent().equals(this.storageFolder.toAbsolutePath())){
                //this is a security check
                throw new RuntimeException(
                        "cannot store file outside current directory");
            }
            try(InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFilePath, StandardCopyOption.REPLACE_EXISTING);
            } // copy file upload vào destinationFilePath // REPLACE_EXISTING nếu tồn tại thì ghi đè
            return generatedFileName2;
        }
        catch (IOException exception) {
            throw new RuntimeException("Failed to store file.",exception);
        }
    }

    @Override
    public Stream<Path> loadAll() {
        try {
            // sử dụng hàm walk duyệt bên trong folder với maxDepth = 1 (duyệt con gần nhất)
            return Files.walk(this.storageFolder, 1)
                    .filter(path -> /*!path.equals(this.storageFoler)*/ !path.equals(this.storageFolder) && !path.toString().contains("._"))
                    .map(this.storageFolder::relativize);
        } // list danh sách các ảnh
        catch (IOException exception) {
            throw new RuntimeException("Failed to load stored files", exception);
        }
    }

    @Override
    public byte[] readFileContent(String fileName) {
        try {
            Path file = storageFolder.resolve(fileName);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                byte[] bytes = StreamUtils.copyToByteArray(resource.getInputStream());
                return bytes;
            }
            else {
                throw new RuntimeException("could not read file: " + fileName);
            }
        }
        catch (IOException exception) {
            throw new RuntimeException("could not read file: "+fileName, exception);
        }
    }

    @Override
    public void deleteFileByName(String fileName) {
        try {
            Path filePath = storageFolder.resolve(fileName);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            } else {
                throw new RuntimeException("File not found: " + fileName);
            }
        } catch (IOException exception) {
            throw new RuntimeException("Failed to delete file: " + fileName, exception);
        }
    }

    @Override
    public void deleteAllFiles() {
        try {
            Files.walk(storageFolder)
                    .filter(path -> !path.equals(storageFolder))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to delete file: " + path.getFileName(), e);
                        }
                    });
        } catch (IOException exception) {
            throw new RuntimeException("Failed to delete files", exception);
        }

    }
}
