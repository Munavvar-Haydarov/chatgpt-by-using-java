package com.unfbx.chatgpt;

import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.unfbx.chatgpt.entity.common.OpenAiResponse;
import com.unfbx.chatgpt.entity.completions.Completion;
import com.unfbx.chatgpt.entity.completions.CompletionResponse;
import com.unfbx.chatgpt.entity.edits.Edit;
import com.unfbx.chatgpt.entity.edits.EditResponse;
import com.unfbx.chatgpt.entity.embeddings.Embedding;
import com.unfbx.chatgpt.entity.embeddings.EmbeddingResponse;
import com.unfbx.chatgpt.entity.engines.Engine;
import com.unfbx.chatgpt.entity.files.File;
import com.unfbx.chatgpt.entity.files.FileDeleteResponse;
import com.unfbx.chatgpt.entity.files.UploadFileResponse;
import com.unfbx.chatgpt.entity.images.*;
import com.unfbx.chatgpt.entity.models.Model;
import com.unfbx.chatgpt.entity.models.ModelResponse;
import com.unfbx.chatgpt.entity.moderations.Moderation;
import com.unfbx.chatgpt.entity.moderations.ModerationResponse;
import com.unfbx.chatgpt.exception.BaseException;
import com.unfbx.chatgpt.exception.CommonError;
import io.reactivex.Single;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


/**
 * 描述： open ai 客户端
 *
 * @author https:www.unfbx.com
 * @date 2023-02-11
 */
@Getter
@Slf4j
public class OpenAiClient {

    private String apiKey;

    private OpenAiApi openAiApi;

    public OpenAiClient(String apiKey) {
        this.apiKey = apiKey;
        this.openAiApi = new Retrofit.Builder()
                .baseUrl("https://api.openai.com/")
                .client(okHttpClient())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create())
                .build().create(OpenAiApi.class);
    }

    /**
     * 创建okhttpClient
     *
     * @return
     */
    private OkHttpClient okHttpClient() {
        OkHttpClient.Builder client = new OkHttpClient.Builder();
        client.addInterceptor(chain -> {
            Request original = chain.request();
            Request request = original.newBuilder()
                    .header(Header.AUTHORIZATION.getValue(), "Bearer " + apiKey)
                    .header(Header.CONTENT_TYPE.getValue(), ContentType.JSON.getValue())
                    .method(original.method(), original.body())
                    .build();
            return chain.proceed(request);
        }).addInterceptor(chain -> {
            Request original = chain.request();
            Response response = chain.proceed(original);
            if (!response.isSuccessful()) {
                if (response.code() == HttpStatus.HTTP_UNAUTHORIZED) {
                    OpenAiResponse body = JSONUtil.toBean(response.body().string(), OpenAiResponse.class);
                    throw new BaseException(body.getError().getMessage());
                }
                String errorMsg = response.body().string();
                log.error("请求异常：{}", errorMsg);
                OpenAiResponse openAiResponse = JSONUtil.toBean(errorMsg, OpenAiResponse.class);
                if (Objects.nonNull(openAiResponse.getError())) {
                    throw new BaseException(openAiResponse.getError().getMessage());
                }
                throw new BaseException(CommonError.RETRY_ERROR);
            }
            return response;
        });
        client.connectTimeout(30, TimeUnit.SECONDS);
        client.writeTimeout(30, TimeUnit.SECONDS);
        client.readTimeout(30, TimeUnit.SECONDS);
        OkHttpClient httpClient = client.build();
        return httpClient;
    }

    /**
     * openAi模型列表
     *
     * @return
     */
    public List<Model> models() {
        Single<ModelResponse> models = this.openAiApi.models();
        List<Model> modelList = models.blockingGet().getData();
        return modelList;
    }

    /**
     * openAi模型详细信息
     *
     * @param id
     * @return
     */
    public Model model(String id) {
        if (Objects.isNull(id) || "".equals(id)) {
            throw new BaseException(CommonError.PARAM_ERROR);
        }
        Single<Model> model = this.openAiApi.model(id);
        return model.blockingGet();
    }


    /**
     * 问答接口
     *
     * @param completion
     * @return
     */
    public CompletionResponse completions(Completion completion) {
        if (Objects.isNull(completion)) {
            throw new BaseException(CommonError.PARAM_ERROR);
        }
        Single<CompletionResponse> completions = this.openAiApi.completions(completion);
        return completions.blockingGet();
    }

    /**
     * 问答接口-简易版
     *
     * @param question
     * @return
     */
    public CompletionResponse completions(String question) {
        Completion q = Completion.builder()
                .prompt(question)
                .build();
        Single<CompletionResponse> completions = this.openAiApi.completions(q);
        return completions.blockingGet();
    }

    /**
     * 文本修改
     *
     * @param edit
     * @return
     */
    public EditResponse edit(Edit edit) {
        Single<EditResponse> edits = this.openAiApi.edits(edit);
        return edits.blockingGet();
    }

    /**
     * 根据描述生成图片
     *
     * @param prompt
     * @return
     */
    public ImageResponse genImages(String prompt) {
        Image image = Image.builder().prompt(prompt).build();
        return this.genImages(image);
    }

    /**
     * 根据描述生成图片
     *
     * @param image
     * @return
     */
    public ImageResponse genImages(Image image) {
        Single<ImageResponse> edits = this.openAiApi.genImages(image);
        return edits.blockingGet();
    }

    /**
     * Creates an edited or extended image given an original image and a prompt.
     * 根据描述修改图片
     *
     * @param image
     * @param prompt
     * @return
     */
    public List<Item> editImages(java.io.File image, String prompt) {
        ImageEdit imageEdit = ImageEdit.builder().prompt(prompt).build();
        return this.editImages(image, null, imageEdit);
    }

    /**
     * Creates an edited or extended image given an original image and a prompt.
     * 根据描述修改图片
     *
     * @param image
     * @param imageEdit
     * @return
     */
    public List<Item> editImages(java.io.File image, ImageEdit imageEdit) {
        return this.editImages(image, null, imageEdit);
    }

    /**
     * Creates an edited or extended image given an original image and a prompt.
     * 根据描述修改图片
     *
     * @param image     png格式的图片，最大4MB
     * @param mask      png格式的图片，最大4MB
     * @param imageEdit
     * @return
     */
    public List<Item> editImages(java.io.File image, java.io.File mask, ImageEdit imageEdit) {
        checkImage(image);
        if (Objects.nonNull(mask) && mask.length() > 4 * 1024 * 1024) {
            log.error("mask最大支持4MB");
            throw new BaseException(CommonError.PARAM_ERROR);
        }
        RequestBody imageBody = RequestBody.create(MediaType.parse("image/png"), image);
        RequestBody maskBody = null;
        if (Objects.nonNull(mask)) {
            maskBody = RequestBody.create(MediaType.parse("image/png"), mask);
        }
        Single<ImageResponse> imageResponse = this.openAiApi.editImages(
                imageBody,
                maskBody,
                imageEdit.getPrompt(),
                imageEdit.getN(),
                imageEdit.getSize(),
                imageEdit.getResponseFormat(),
                imageEdit.getUser());
        return imageResponse.blockingGet().getData();
    }

    /**
     * Creates a variation of a given image.
     *
     * @param image
     * @param imageVariations
     * @return
     */
    public ImageResponse variationsImages(java.io.File image, ImageVariations imageVariations) {
        checkImage(image);
        RequestBody imageBody = RequestBody.create(MediaType.parse("image/png"), image);
        Single<ImageResponse> variationsImages = this.openAiApi.variationsImages(
                imageBody,
                imageVariations.getN(),
                imageVariations.getSize(),
                imageVariations.getResponseFormat(),
                imageVariations.getUser()
        );
        return variationsImages.blockingGet();
    }

    /**
     * Creates a variation of a given image.
     *
     * @param image
     * @return
     */
    public ImageResponse variationsImages(java.io.File image) {
        checkImage(image);
        ImageVariations imageVariations = ImageVariations.builder().build();
        RequestBody imageBody = RequestBody.create(MediaType.parse("image/png"), image);
        Single<ImageResponse> variationsImages = this.openAiApi.variationsImages(
                imageBody,
                imageVariations.getN(),
                imageVariations.getSize(),
                imageVariations.getResponseFormat(),
                imageVariations.getUser()
        );
        return variationsImages.blockingGet();
    }

    /**
     * 校验图片信息
     *
     * @param image
     */
    private void checkImage(java.io.File image) {
        if (Objects.isNull(image)) {
            log.error("image不能为空");
            throw new BaseException(CommonError.PARAM_ERROR);
        }
        if (!(image.getName().endsWith("png") || image.getName().endsWith("PNG"))) {
            log.error("image格式错误");
            throw new BaseException(CommonError.PARAM_ERROR);
        }
        if (image.length() > 4 * 1024 * 1024) {
            log.error("image最大支持4MB");
            throw new BaseException(CommonError.PARAM_ERROR);
        }
    }

    /**
     * Creates an embedding vector representing the input text.
     * @param input
     * @return
     */
    public EmbeddingResponse embeddings(String input) {
        Embedding embedding = Embedding.builder().input(input).build();
        return this.embeddings(embedding);
    }

    /**
     * Creates an embedding vector representing the input text.
     * @param embedding
     * @return
     */
    public EmbeddingResponse embeddings(Embedding embedding) {
        Single<EmbeddingResponse> embeddings = this.openAiApi.embeddings(embedding);
        return embeddings.blockingGet();
    }

    /**
     * 获取文件列表
     * @return
     */
    public List<File> files() {
        Single<OpenAiResponse<File>> files = this.openAiApi.files();
        return files.blockingGet().getData();
    }

    /**
     * 删除文件
     * @param fileId
     * @return
     */
    public FileDeleteResponse deleteFile(String fileId) {
        Single<FileDeleteResponse> deleteFile = this.openAiApi.deleteFile(fileId);
        return deleteFile.blockingGet();
    }

    /**
     * 上传文件
     * @param purpose
     * @param file
     * @return
     */
    public UploadFileResponse uploadFile(String purpose, RequestBody file) {
        Single<UploadFileResponse> uploadFileResponse = this.openAiApi.uploadFile(purpose, file);
        return uploadFileResponse.blockingGet();
    }

    /**
     * 上传文件
     * @param file
     * @return
     */
    public UploadFileResponse uploadFile(RequestBody file) {
        //purpose 官网示例默认是：fine-tune
        return this.uploadFile("fine-tune", file);
    }

    /**
     * 检索文件
     * @param fileId
     * @return
     */
    public File retrieveFile(String fileId) {
        Single<File> fileContent = this.openAiApi.retrieveFile(fileId);
        return fileContent.blockingGet();
    }

    /**
     * 检索文件内容
     * 免费用户无法使用此接口
     * @param fileId
     * @return
     */
    public ResponseBody retrieveFileContent(String fileId) {
        Single<ResponseBody> fileContent = this.openAiApi.retrieveFileContent(fileId);
        return fileContent.blockingGet();
    }

    /**
     * 文本审核
     * @param input
     * @return
     */
    public ModerationResponse moderations(String input) {
        Moderation moderation = Moderation.builder().input(input).build();
        return this.moderations(moderation);
    }

    /**
     * 文本审核
     * @param moderation
     * @return
     */
    public ModerationResponse moderations(Moderation moderation) {
        Single<ModerationResponse> moderations = this.openAiApi.moderations(moderation);
        return moderations.blockingGet();
    }

    /**
     * 引擎列表
     * @return
     */
    @Deprecated
    public List<Engine> engines() {
        Single<OpenAiResponse<Engine>> engines = this.openAiApi.engines();
        return engines.blockingGet().getData();
    }

    /**
     * 引擎详细信息
     * @param engineId
     * @return
     */
    @Deprecated
    public Engine engine(String engineId) {
        Single<Engine> engine = this.openAiApi.engine(engineId);
        return engine.blockingGet();
    }
}
